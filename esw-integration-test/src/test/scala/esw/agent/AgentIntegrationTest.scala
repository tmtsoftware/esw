package esw.agent

import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.{Machine, SequenceComponent, Service}
import csw.location.api.models.Connection.{AkkaConnection, TcpConnection}
import csw.prefix.models.Prefix
import esw.BinaryFetcherUtil
import esw.agent.api.ComponentStatus.Running
import esw.agent.api.{AgentStatus, Failed, Killed, Spawned}
import esw.agent.app.AgentSettings
import esw.agent.client.AgentClient
import esw.ocs.testkit.EswTestKit

import scala.concurrent.duration.DurationLong

class AgentIntegrationTest extends EswTestKit with BinaryFetcherUtil with LocationServiceCodecs {

  private val irisPrefix               = Prefix("esw.iris")
  private val irisCompId               = ComponentId(irisPrefix, SequenceComponent)
  private val irisSeqCompConnection    = AkkaConnection(ComponentId(irisPrefix, SequenceComponent))
  private val redisPrefix              = Prefix(s"esw.event_server")
  private val redisCompId              = ComponentId(redisPrefix, Service)
  private val appVersion               = Some("d94b7c56e3")
  private var agentPrefix: Prefix      = _
  private var agentClient: AgentClient = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    val channel: String = "file://" + getClass.getResource("/apps.json").getPath
    agentPrefix = spawnAgent(AgentSettings(1.minute, channel))
    super.fetchBinaryFor(channel, appVersion)
    agentClient = AgentClient.make(agentPrefix, locationService).futureValue
  }

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(1.minute, 100.millis)

  //ESW-325: spawns sequence component via agent using coursier with provided sha
  private def spawnSequenceComponent(prefix: Prefix) = agentClient.spawnSequenceComponent(prefix, appVersion)

  "Agent" must {
    "start and register itself with location service | ESW-237" in {
      val agentLocation = locationService.resolve(AkkaConnection(ComponentId(agentPrefix, Machine)), 5.seconds).futureValue
      agentLocation should not be empty
    }

    "return Spawned on SpawnSequenceComponent and Killed on KillComponent message | ESW-237, ESW-276, ESW-325" in {
      spawnSequenceComponent(irisPrefix).futureValue should ===(Spawned)
      // Verify registration in location service
      locationService.resolve(irisSeqCompConnection, 5.seconds).futureValue should not be empty

      agentClient.killComponent(ComponentId(irisPrefix, SequenceComponent)).futureValue should ===(Killed)
      // Verify not registered in location service
      locationService.resolve(irisSeqCompConnection, 5.seconds).futureValue shouldEqual None
    }

    "return Spawned after spawning a new redis component for a SpawnRedis message | ESW-237, ESW-325" in {
      agentClient.spawnRedis(redisPrefix, 6380, List.empty).futureValue should ===(Spawned)
      // Verify registration in location service
      locationService.resolve(TcpConnection(ComponentId(redisPrefix, Service)), 5.seconds).futureValue should not be empty

      agentClient.killComponent(ComponentId(redisPrefix, Service)).futureValue
    }

    "return Failed('Aborted') to original sender when someone kills a process while it is spawning | ESW-237, ESW-237" in {
      val spawnResponseF = spawnSequenceComponent(irisPrefix)
      agentClient.killComponent(irisCompId).futureValue should ===(Killed)
      spawnResponseF.futureValue should ===(Failed("Aborted"))
      // Verify not registered in location service
      locationService.resolve(irisSeqCompConnection, 5.seconds).futureValue should ===(None)
    }

    "return status of components available on agent for a GetAgentStatus message | ESW-286" in {
      spawnSequenceComponent(irisPrefix).futureValue should ===(Spawned)
      agentClient.getComponentStatus(irisCompId).futureValue should ===(Running)

      agentClient.spawnRedis(redisPrefix, 6381, List.empty).futureValue should ===(Spawned)
      agentClient.getComponentStatus(redisCompId).futureValue should ===(Running)

      val agentStatus = agentClient.getAgentStatus.futureValue
      agentStatus should ===(AgentStatus(Map(irisCompId -> Running, redisCompId -> Running)))

      // cleanup
      agentClient.killComponent(irisCompId).futureValue
      agentClient.killComponent(ComponentId(redisPrefix, Service)).futureValue
    }
  }

  override def afterAll(): Unit = {
    locationService.unregisterAll()
    super.afterAll()
  }
}

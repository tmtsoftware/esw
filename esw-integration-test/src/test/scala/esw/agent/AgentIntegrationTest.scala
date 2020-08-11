package esw.agent

import java.nio.file.Paths

import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.{Machine, SequenceComponent, Service}
import csw.location.api.models.Connection.{AkkaConnection, TcpConnection}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, IRIS}
import esw.agent.akka.app.AgentSettings
import esw.agent.akka.app.process.cs.Coursier
import esw.agent.akka.client.AgentClient
import esw.agent.service.api.models.ComponentStatus.Running
import esw.agent.service.api.models.{AgentStatus, Killed, Spawned}
import esw.ocs.api.actor.client.SequenceComponentImpl
import esw.ocs.api.models.ObsMode
import esw.ocs.api.protocol.SequenceComponentResponse.SequencerLocation
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.AAS
import esw.{BinaryFetcherUtil, GitUtil}

import scala.concurrent.duration.DurationLong

class AgentIntegrationTest extends EswTestKit(AAS) with LocationServiceCodecs {

  private val irisPrefix               = Prefix("esw.iris")
  private val irisCompId               = ComponentId(irisPrefix, SequenceComponent)
  private val irisSeqCompConnection    = AkkaConnection(ComponentId(irisPrefix, SequenceComponent))
  private val redisPrefix              = Prefix(s"esw.event_server")
  private val redisCompId              = ComponentId(redisPrefix, Service)
  private val appVersion               = GitUtil.latestCommitSHA("esw")
  private var agentPrefix: Prefix      = _
  private var agentClient: AgentClient = _

  private val eswVersion: Some[String] = Some(appVersion)

  override def beforeAll(): Unit = {
    super.beforeAll()
    val channel: String = "file://" + getClass.getResource("/apps.json").getPath
    agentPrefix = spawnAgent(AgentSettings(1.minute, channel))
    BinaryFetcherUtil.fetchBinaryFor(channel, Coursier.ocsApp(eswVersion), eswVersion)
    BinaryFetcherUtil.fetchBinaryFor(channel, Coursier.smApp(eswVersion), eswVersion)
    agentClient = AgentClient.make(agentPrefix, locationService).futureValue
  }

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(1.minute, 100.millis)

  //ESW-325: spawns sequence component via agent using coursier with provided sha
  private def spawnSequenceComponent(prefix: Prefix) = agentClient.spawnSequenceComponent(prefix, eswVersion)

  "Agent" must {
    "start and register itself with location service | ESW-237" in {
      val agentLocation = locationService.resolve(AkkaConnection(ComponentId(agentPrefix, Machine)), 5.seconds).futureValue
      agentLocation should not be empty
    }

    "return Spawned on SpawnSequenceComponent and Killed on KillComponent message |  ESW-153, ESW-237, ESW-276, ESW-325" in {
      val darknight = ObsMode("darknight")
      spawnSequenceComponent(irisPrefix).futureValue should ===(Spawned)
      // Verify registration in location service
      val seqCompLoc = locationService.resolve(irisSeqCompConnection, 5.seconds).futureValue.value
      seqCompLoc.connection shouldBe irisSeqCompConnection

      // start sequencer i.e. load IRIS darknight script
      val seqCompApi         = new SequenceComponentImpl(seqCompLoc)
      val loadScriptResponse = seqCompApi.loadScript(IRIS, darknight).futureValue

      // verify sequencer location from load script and looked up from location service is the same
      loadScriptResponse shouldBe SequencerLocation(resolveSequencerLocation(IRIS, darknight))

      agentClient.killComponent(ComponentId(irisPrefix, SequenceComponent)).futureValue should ===(Killed)
      // Verify not registered in location service
      locationService.resolve(irisSeqCompConnection, 5.seconds).futureValue shouldEqual None
    }

    "return Spawned on SpawnSequenceManager | ESW-180" in {
      val obsModeConfigPath = Paths.get(ClassLoader.getSystemResource("smObsModeConfig.conf").toURI)
      // spawn sequence manager
      agentClient.spawnSequenceManager(obsModeConfigPath, isConfigLocal = true, eswVersion).futureValue should ===(Spawned)

      // Verify registration in location service
      val seqManagerConnection = AkkaConnection(ComponentId(Prefix(ESW, "sequence_manager"), Service))
      locationService.resolve(seqManagerConnection, 5.seconds).futureValue.value

      agentClient.killComponent(ComponentId(Prefix(ESW, "sequence_manager"), Service)).futureValue
    }

    "return Spawned after spawning a new redis component for a SpawnRedis message | ESW-237, ESW-325" in {
      agentClient.spawnRedis(redisPrefix, 6380, List.empty).futureValue should ===(Spawned)
      // Verify registration in location service
      locationService.resolve(TcpConnection(ComponentId(redisPrefix, Service)), 5.seconds).futureValue should not be empty

      agentClient.killComponent(ComponentId(redisPrefix, Service)).futureValue
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

package esw.agent

import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.{Machine, SequenceComponent, Service}
import csw.location.api.models.Connection.{AkkaConnection, TcpConnection}
import csw.prefix.models.Prefix
import esw.agent.api.ComponentStatus.Running
import esw.agent.api.{AgentStatus, Failed, Killed, Spawned}
import esw.agent.client.AgentClient
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.MachineAgent
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.duration.DurationLong

class AgentIntegrationTest extends EswTestKit(MachineAgent) with BeforeAndAfterAll with LocationServiceCodecs {

  private lazy val agentClient      = AgentClient.make(agentPrefix, locationService).futureValue
  private val irisPrefix            = Prefix("esw.iris")
  private val irisCompId            = ComponentId(irisPrefix, SequenceComponent)
  private val irisSeqCompConnection = AkkaConnection(ComponentId(irisPrefix, SequenceComponent))
  private val redisPrefix           = Prefix(s"esw.event_server")
  private val redisCompId           = ComponentId(redisPrefix, Service)

  "Agent" must {
    "start and register itself with location service | ESW-237" in {
      val agentLocation = locationService.resolve(AkkaConnection(ComponentId(agentPrefix, Machine)), 5.seconds).futureValue
      agentLocation should not be empty
    }

    "return Spawned on SpawnSequenceComponent and Killed on KillComponent message | ESW-237, ESW-276" in {
      agentClient.spawnSequenceComponent(irisPrefix).futureValue should ===(Spawned)
      // Verify registration in location service
      locationService.resolve(irisSeqCompConnection, 5.seconds).futureValue should not be empty

      agentClient.killComponent(ComponentId(irisPrefix, SequenceComponent)).futureValue should ===(Killed.gracefully)
      // Verify not registered in location service
      locationService.resolve(irisSeqCompConnection, 5.seconds).futureValue shouldEqual None
    }

    "return Spawned after spawning a new redis component for a SpawnRedis message | ESW-237" in {
      agentClient.spawnRedis(redisPrefix, 6379, List.empty).futureValue should ===(Spawned)
      // Verify registration in location service
      locationService.resolve(TcpConnection(ComponentId(redisPrefix, Service)), 5.seconds).futureValue should not be empty
    }

    // todo: see if we really need killedForcefully and is there a way to write test for that with coursier approach?
//    "return killedForcefully after killing a registered component for a killComponent message | ESW-276" in {
//      val seqCompPrefix     = Prefix(s"esw.test_${Random.nextInt().abs}_delay_exit")
//      val seqCompId         = ComponentId(seqCompPrefix, SequenceComponent)
//      val seqCompConnection = AkkaConnection(seqCompId)
//
//      agentClient.spawnSequenceComponent(seqCompPrefix).futureValue should ===(Spawned)
//      agentClient.killComponent(seqCompId).futureValue should ===(Killed.forcefully)
//      // Verify not registered in location service
//      locationService.resolve(seqCompConnection, 5.seconds).futureValue shouldEqual None
//    }

    "return Failed('Aborted') to original sender when someone kills a process while it is spawning | ESW-237, ESW-237" in {
      val spawnResponseF = agentClient.spawnSequenceComponent(irisPrefix)
      agentClient.killComponent(irisCompId).futureValue should ===(Killed.gracefully)
      spawnResponseF.futureValue should ===(Failed("Aborted"))
      // Verify not registered in location service
      locationService.resolve(irisSeqCompConnection, 5.seconds).futureValue should ===(None)
    }

    "return status of components available on agent for a GetAgentStatus message | ESW-286" in {
      agentClient.spawnSequenceComponent(irisPrefix).futureValue should ===(Spawned)
      agentClient.getComponentStatus(irisCompId).futureValue should ===(Running)

      agentClient.spawnRedis(redisPrefix, 100, List.empty).futureValue should ===(Spawned)
      agentClient.getComponentStatus(redisCompId).futureValue should ===(Running)

      val agentStatus = agentClient.getAgentStatus.futureValue
      agentStatus should ===(AgentStatus(Map(irisCompId -> Running, redisCompId -> Running)))

      // cleanup
      agentClient.killComponent(irisCompId).futureValue
    }
  }

  override def afterAll(): Unit = {
    locationService.unregisterAll()
    super.afterAll()
  }
}

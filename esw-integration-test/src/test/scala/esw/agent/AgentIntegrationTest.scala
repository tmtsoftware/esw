package esw.agent

import akka.util.Timeout
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

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong
import scala.util.Random

class AgentIntegrationTest extends EswTestKit(MachineAgent) with BeforeAndAfterAll with LocationServiceCodecs {
  override implicit lazy val askTimeout: Timeout = 5.seconds

  "Agent" must {
    "start and register itself with location service | ESW-237" in {
      val agentLocation = locationService.resolve(AkkaConnection(ComponentId(agentPrefix, Machine)), 5.seconds).futureValue
      agentLocation should not be empty
    }

    "return Spawned after spawning a new sequence component for a SpawnSequenceComponent message | ESW-237" in {
      val agentClient   = Await.result(AgentClient.make(agentPrefix, locationService), 7.seconds)
      val seqCompPrefix = Prefix(s"esw.test_${Random.nextInt.abs}")
      val response      = Await.result(agentClient.spawnSequenceComponent(seqCompPrefix), askTimeout.duration)
      response should ===(Spawned)
      // Verify registration in location service
      locationService
        .resolve(AkkaConnection(ComponentId(seqCompPrefix, SequenceComponent)), 5.seconds)
        .futureValue should not be empty
    }

    "return Spawned after spawning a new redis component for a SpawnRedis message | ESW-237" in {
      val agentClient = Await.result(AgentClient.make(agentPrefix, locationService), 7.seconds)
      val prefix      = Prefix(s"esw.event_server")
      val response    = Await.result(agentClient.spawnRedis(prefix, 6379, List.empty), askTimeout.duration)
      response should ===(Spawned)
      // Verify registration in location service
      locationService.resolve(TcpConnection(ComponentId(prefix, Service)), 5.seconds).futureValue should not be empty
    }

    "return killedGracefully after killing a registered component for a KillComponent message | ESW-276" in {
      val agentClient   = Await.result(AgentClient.make(agentPrefix, locationService), 7.seconds)
      val seqCompPrefix = Prefix(s"esw.test_${Random.nextInt.abs}")
      val spawnResponse = Await.result(agentClient.spawnSequenceComponent(seqCompPrefix), askTimeout.duration)
      spawnResponse should ===(Spawned)
      val killResponse =
        Await.result(agentClient.killComponent(ComponentId(seqCompPrefix, SequenceComponent)), askTimeout.duration)
      killResponse should ===(Killed.gracefully)
      // Verify not registered in location service
      locationService
        .resolve(AkkaConnection(ComponentId(seqCompPrefix, SequenceComponent)), 5.seconds)
        .futureValue shouldEqual None
    }

    "return killedForcefully after killing a registered component for a killComponent message | ESW-276" in {
      val agentClient   = Await.result(AgentClient.make(agentPrefix, locationService), 7.seconds)
      val seqCompPrefix = Prefix(s"esw.test_${Random.nextInt.abs}_delay_exit")
      val spawnResponse = Await.result(agentClient.spawnSequenceComponent(seqCompPrefix), askTimeout.duration)
      spawnResponse should ===(Spawned)
      val killResponse =
        Await.result(agentClient.killComponent(ComponentId(seqCompPrefix, SequenceComponent)), askTimeout.duration)
      killResponse should ===(Killed.forcefully)
      // Verify not registered in location service
      locationService
        .resolve(AkkaConnection(ComponentId(seqCompPrefix, SequenceComponent)), 5.seconds)
        .futureValue shouldEqual None
    }

    "return Failed('Aborted') to original sender when someone kills a process while it is spawning | ESW-237, ESW-237" in {
      val agentClient    = Await.result(AgentClient.make(agentPrefix, locationService), 7.seconds)
      val seqCompPrefix  = Prefix(s"esw.test_${Random.nextInt.abs}")
      val spawnResponseF = agentClient.spawnSequenceComponent(seqCompPrefix)
      val killResponse =
        Await.result(agentClient.killComponent(ComponentId(seqCompPrefix, SequenceComponent)), askTimeout.duration)
      killResponse should ===(Killed.gracefully)
      Await.result(spawnResponseF, askTimeout.duration) should ===(Failed("Aborted"))
      // Verify not registered in location service
      locationService
        .resolve(AkkaConnection(ComponentId(seqCompPrefix, SequenceComponent)), 5.seconds)
        .futureValue shouldEqual None
    }

    "return status of components available on agent for a GetAgentStatus message | ESW-286" in {
      val agentClient = Await.result(AgentClient.make(agentPrefix, locationService), 7.seconds)

      val seqCompPrefix        = Prefix(s"esw.test_${Random.nextInt.abs}_delay_exit")
      val seqCompSpawnResponse = Await.result(agentClient.spawnSequenceComponent(seqCompPrefix), askTimeout.duration)
      seqCompSpawnResponse should ===(Spawned)
      val seqComponentId = ComponentId(seqCompPrefix, SequenceComponent)
      val seqCompStatus  = Await.result(agentClient.getComponentStatus(seqComponentId), 500.millis)
      seqCompStatus should ===(Running)

      val redisPrefix        = Prefix(s"esw.test_${Random.nextInt.abs}_delay_exit")
      val redisSpawnResponse = Await.result(agentClient.spawnRedis(redisPrefix, 100, List.empty), askTimeout.duration)
      redisSpawnResponse should ===(Spawned)
      val redisCompId     = ComponentId(redisPrefix, Service)
      val redisCompStatus = Await.result(agentClient.getComponentStatus(redisCompId), 500.millis)
      redisCompStatus should ===(Running)

      val agentStatus = Await.result(agentClient.getAgentStatus, 500.millis)
      agentStatus should ===(AgentStatus(Map(seqComponentId -> Running, redisCompId -> Running)))
    }
  }

  override def afterAll(): Unit = {
    locationService.unregisterAll()
    super.afterAll()
  }
}

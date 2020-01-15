package esw.agent

import akka.util.Timeout
import csw.location.api.codec.LocationServiceCodecs
import csw.location.models.ComponentId
import csw.location.models.ComponentType.SequenceComponent
import csw.prefix.models.Prefix
import esw.agent.api.Response.{Failed, Ok}
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
      //this asserts agent has started and registered in location service
      agentLocation should not be empty
    }

    "return OK and spawn a new sequence component for a SpawnSequenceComponent message | ESW-237" in {
      val agentClient   = Await.result(AgentClient.make(agentPrefix, locationService), 7.seconds)
      val seqCompPrefix = Prefix(s"esw.test_${Random.nextInt.abs}")
      val response      = Await.result(agentClient.spawnSequenceComponent(seqCompPrefix), askTimeout.duration)
      response should ===(Ok)
    }

    "return Ok and kill a running component for a KillComponent message | ESW-237" in {
      val agentClient   = Await.result(AgentClient.make(agentPrefix, locationService), 7.seconds)
      val seqCompPrefix = Prefix(s"esw.test_${Random.nextInt.abs}")
      val spawnResponse = Await.result(agentClient.spawnSequenceComponent(seqCompPrefix), askTimeout.duration)
      spawnResponse should ===(Ok)
      val killResponse =
        Await.result(agentClient.killComponent(ComponentId(seqCompPrefix, SequenceComponent)), askTimeout.duration)
      killResponse should ===(Ok)
    }

    "return Failed('Aborted') to original sender when someone kills a process while it is spawning | ESW-237" in {
      val agentClient    = Await.result(AgentClient.make(agentPrefix, locationService), 7.seconds)
      val seqCompPrefix  = Prefix(s"esw.test_${Random.nextInt.abs}")
      val spawnResponseF = agentClient.spawnSequenceComponent(seqCompPrefix)
      val killResponse =
        Await.result(agentClient.killComponent(ComponentId(seqCompPrefix, SequenceComponent)), askTimeout.duration)
      killResponse should ===(Ok)
      Await.result(spawnResponseF, askTimeout.duration) should ===(Failed("Aborted"))
    }
  }

  override def afterAll(): Unit = {
    locationService.unregisterAll()
    super.afterAll()
  }
}

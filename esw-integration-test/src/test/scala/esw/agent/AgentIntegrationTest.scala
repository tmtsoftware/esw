package esw.agent

import esw.agent.api.Response.Spawned
import akka.util.Timeout
import csw.location.api.codec.LocationServiceCodecs
import csw.prefix.models.Prefix
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

    "accept SpawnSequenceComponent message and spawn a new sequence component | ESW-237" in {
      val agentClient   = Await.result(AgentClient.make(agentPrefix, locationService), 7.seconds)
      val seqCompPrefix = Prefix(s"esw.test_${Random.nextInt.abs}")
      val response      = Await.result(agentClient.spawnSequenceComponent(seqCompPrefix), askTimeout.duration)
      response should ===(Spawned)
    }
  }

  override def afterAll(): Unit = {
    locationService.unregisterAll()
    super.afterAll()
  }
}

package esw.agent

import agent.api.AgentCommand
import agent.api.AgentCommand.SpawnCommand.SpawnSequenceComponent
import agent.api.Response.Spawned
import akka.actor.typed.Scheduler
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.util.Timeout
import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.extensions.URIExtension.RichURI
import csw.prefix.models.Prefix
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.MachineAgent
import org.scalatest.BeforeAndAfterEach

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong
import scala.util.Random

class AgentIntegrationTest extends EswTestKit(MachineAgent) with BeforeAndAfterEach with LocationServiceCodecs {
  override implicit lazy val askTimeout: Timeout = 5.seconds

  "Agent" must {
    "start and register itself with location service | ESW-237" in {
      //this asserts agent has started and registered in location service
      agentLocation should not be empty
    }

    "accept SpawnSequenceComponent message and spawn a new sequence component | ESW-237" in {
      val agentUri                      = agentLocation.get.uri
      val agentRef                      = agentUri.toActorRef.unsafeUpcast[AgentCommand]
      implicit val scheduler: Scheduler = system.scheduler
      val seqCompPrefix                 = Prefix(s"esw.test_${Random.nextInt.abs}")
      val response                      = Await.result(agentRef ? SpawnSequenceComponent(seqCompPrefix), askTimeout.duration)
      response should ===(Spawned)
    }
  }

  override def afterEach(): Unit = locationService.unregisterAll()
}

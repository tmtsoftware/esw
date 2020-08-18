package esw.agent.akka.client

import java.net.URI
import java.nio.file.Path

import akka.actor.typed.ActorRef
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.models.ComponentType.{Machine, SequenceComponent}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, Metadata}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.akka.client.AgentCommand.KillComponent
import esw.agent.akka.client.AgentCommand.SpawnCommand.{SpawnSequenceComponent, SpawnSequenceManager}
import esw.agent.service.api.models.{Killed, Spawned}
import esw.commons.utils.location.EswLocationError.LocationNotFound
import esw.commons.utils.location.LocationServiceUtil
import esw.testcommons.{ActorTestSuit, AskProxyTestKit}

import scala.concurrent.Future
import scala.util.Random

class AgentClientTest extends ActorTestSuit {

  private val agentPrefix = Prefix(ESW, "agent")
  private val askProxyTestKit: AskProxyTestKit[AgentCommand, AgentClient] = new AskProxyTestKit[AgentCommand, AgentClient] {
    override def make(actorRef: ActorRef[AgentCommand]): AgentClient = {
      val location =
        AkkaLocation(
          AkkaConnection(ComponentId(agentPrefix, Machine)),
          actorRef.toURI,
          Metadata.empty
        )
      new AgentClient(location)
    }
  }

  import askProxyTestKit._

  private def randomString5 = Random.nextString(5)

  "make" should {
    "resolve the given prefix and return a new instance of AgentClient  | ESW-237" in {
      val locationService: LocationServiceUtil = mock[LocationServiceUtil]
      val prefix                               = Prefix("esw.test1")
      val akkaConnection                       = AkkaConnection(ComponentId(prefix, Machine))
      val agentLocation                        = AkkaLocation(akkaConnection, URI.create("akka://abc"), Metadata.empty)
      when(locationService.find(akkaConnection)).thenReturn(Future.successful(Right(agentLocation)))
      AgentClient.make(prefix, locationService).futureValue
    }

    "return a failed future when location service cant resolve agent  | ESW-237" in {
      val locationService: LocationServiceUtil = mock[LocationServiceUtil]
      val prefix                               = Prefix("esw.test1")
      val akkaConnection                       = AkkaConnection(ComponentId(prefix, Machine))
      when(locationService.find(akkaConnection)).thenReturn(Future.successful(Left(LocationNotFound("error"))))
      val exception = intercept[RuntimeException](AgentClient.make(prefix, locationService).futureValue)
      exception.getCause.getMessage should ===(s"could not resolve agent with prefix: $prefix")
    }

    "return a failed future when location service call fails  | ESW-237" in {
      val locationService: LocationServiceUtil = mock[LocationServiceUtil]
      val prefix                               = Prefix("esw.test1")
      val akkaConnection                       = AkkaConnection(ComponentId(prefix, Machine))
      when(locationService.find(akkaConnection)).thenReturn(Future.failed(new RuntimeException("boom")))
      val exception = intercept[RuntimeException](AgentClient.make(prefix, locationService).futureValue)
      exception.getCause.getMessage should ===(s"boom")
    }
  }

  "spawnSequenceComponent" should {
    "send SpawnSequenceComponent message to agent and return a future with agent response" in {
      val prefix        = Prefix(s"esw.$randomString5")
      val componentName = prefix.componentName
      withBehavior {
        case SpawnSequenceComponent(replyTo, `agentPrefix`, `componentName`, None) => replyTo ! Spawned
      } check { ac =>
        ac.spawnSequenceComponent(componentName).futureValue should ===(Spawned)
      }
    }
  }

  "killComponent" should {
    "send KillComponent message to agent and return a future with agent response" in {
      val location =
        AkkaLocation(AkkaConnection(ComponentId(Prefix("IRIS.filter"), SequenceComponent)), new URI("uri"), Metadata.empty)

      withBehavior {
        case KillComponent(replyTo, `location`) => replyTo ! Killed
      } check { ac =>
        ac.killComponent(location).futureValue should ===(Killed)
      }
    }
  }

  "spawnSequenceManager" should {
    "send spawnSequenceManager message to agent and return a future with agent response | ESW-180" in {
      val configPath = Path.of("obsMode.conf")
      withBehavior {
        case SpawnSequenceManager(replyTo, `configPath`, true, None) => replyTo ! Spawned
      } check { ac =>
        ac.spawnSequenceManager(configPath, isConfigLocal = true).futureValue should ===(Spawned)
      }
    }
  }
}

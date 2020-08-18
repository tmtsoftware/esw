package esw.agent.akka.client

import java.net.URI
import java.nio.file.Path

import akka.actor.typed.ActorRef
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.models.ComponentType.{Machine, SequenceComponent, Service}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, Metadata}
import csw.location.api.scaladsl.LocationService
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.akka.client.AgentCommand.SpawnCommand.SpawnManuallyRegistered.SpawnRedis
import esw.agent.akka.client.AgentCommand.SpawnCommand.SpawnSelfRegistered.{SpawnSequenceComponent, SpawnSequenceManager}
import esw.agent.akka.client.AgentCommand.{GetAgentStatus, GetComponentStatus, KillComponent}
import esw.agent.service.api.models.ComponentStatus.{Running, Stopping}
import esw.agent.service.api.models.{AgentStatus, Killed, Spawned}
import esw.testcommons.{ActorTestSuit, AskProxyTestKit}

import scala.concurrent.Future

class AgentClientTest extends ActorTestSuit {

  private val askProxyTestKit: AskProxyTestKit[AgentCommand, AgentClient] = new AskProxyTestKit[AgentCommand, AgentClient] {
    override def make(actorRef: ActorRef[AgentCommand]): AgentClient = {
      val location =
        AkkaLocation(
          AkkaConnection(ComponentId(Prefix(ESW, "agent"), Machine)),
          actorRef.toURI,
          Metadata.empty
        )
      new AgentClient(location)
    }
  }
  import askProxyTestKit._

  "make" should {
    "resolve the given prefix and return a new instance of AgentClient  | ESW-237" in {
      val locationService: LocationService = mock[LocationService]
      val prefix                           = Prefix("esw.test1")
      val akkaConnection                   = AkkaConnection(ComponentId(prefix, Machine))
      val agentLocation                    = AkkaLocation(akkaConnection, URI.create("akka://abc"), Metadata.empty)
      when(locationService.find(akkaConnection)).thenReturn(Future.successful(Some(agentLocation)))
      AgentClient.make(prefix, locationService).futureValue
    }

    "return a failed future when location service cant resolve agent  | ESW-237" in {
      val locationService: LocationService = mock[LocationService]
      val prefix                           = Prefix("esw.test1")
      val akkaConnection                   = AkkaConnection(ComponentId(prefix, Machine))
      when(locationService.find(akkaConnection)).thenReturn(Future.successful(None))
      val exception = intercept[RuntimeException](AgentClient.make(prefix, locationService).futureValue)
      exception.getCause.getMessage should ===(s"could not resolve agent with prefix: $prefix")
    }

    "return a failed future when location service call fails  | ESW-237" in {
      val locationService: LocationService = mock[LocationService]
      val prefix                           = Prefix("esw.test1")
      val akkaConnection                   = AkkaConnection(ComponentId(prefix, Machine))
      when(locationService.find(akkaConnection)).thenReturn(Future.failed(new RuntimeException("boom")))
      val exception = intercept[RuntimeException](AgentClient.make(prefix, locationService).futureValue)
      exception.getCause.getMessage should ===(s"boom")
    }
  }

  "spawnSequenceComponent" should {
    "send SpawnSequenceComponent message to agent and return a future with agent response" in {
      val prefix = Prefix("esw.test2")
      withBehavior {
        case SpawnSequenceComponent(replyTo, _, _, _) => replyTo ! Spawned
      } check { ac =>
        ac.spawnSequenceComponent(prefix.componentName).futureValue should ===(Spawned)
      }
    }
  }

  "spawnRedis" should {
    "send SpawnRedis message to agent and return a future with agent response" in {
      val prefix = Prefix("esw.test3")
      withBehavior {
        case SpawnRedis(replyTo, _, _, _) => replyTo ! Spawned
      } check { ac =>
        ac.spawnRedis(prefix, 6379, List("--port", "6379")).futureValue should ===(Spawned)
      }
    }
  }

  "killComponent" should {
    "send KillComponent message to agent and return a future with agent response" in {
      val componentId = ComponentId(Prefix("esw.test3"), SequenceComponent)
      withBehavior {
        case KillComponent(replyTo, _) => replyTo ! Killed
      } check { ac =>
        ac.killComponent(componentId).futureValue should ===(Killed)
      }
    }
  }

  "getComponentStatus" should {
    "send GetComponentStatus message to agent and return a future with agent response" in {
      val componentId = ComponentId(Prefix("esw.test3"), SequenceComponent)
      withBehavior {
        case GetComponentStatus(replyTo, _) => replyTo ! Running
      } check { ac =>
        ac.getComponentStatus(componentId).futureValue should ===(Running)
      }
    }
  }

  "getAgentStatus" should {
    "send GetAgentStatus message to agent and return a future with agent response" in {
      val componentId = ComponentId(Prefix("esw.comp"), Service)
      withBehavior {
        case GetAgentStatus(replyTo) => replyTo ! AgentStatus(Map(ComponentId(Prefix("esw.comp"), Service) -> Stopping))
      } check { ac =>
        ac.getAgentStatus.futureValue should ===(AgentStatus(Map(componentId -> Stopping)))
      }
    }
  }

  "spawnSequenceManager" should {
    "send spawnSequenceManager message to agent and return a future with agent response | ESW-180" in {
      withBehavior {
        case SpawnSequenceManager(replyTo, _, _, _) => replyTo ! Spawned
      } check { ac =>
        ac.spawnSequenceManager(Path.of("obsMode.conf"), isConfigLocal = false).futureValue should ===(Spawned)
      }
    }
  }
}

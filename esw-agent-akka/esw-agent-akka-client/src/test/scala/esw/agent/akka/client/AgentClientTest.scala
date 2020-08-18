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
      val prefix        = Prefix(s"esw.$randomString5")
      val componentName = prefix.componentName
      withBehavior {
        case SpawnSequenceComponent(replyTo, `agentPrefix`, `componentName`, None) => replyTo ! Spawned
      } check { ac =>
        ac.spawnSequenceComponent(componentName).futureValue should ===(Spawned)
      }
    }
  }

  "spawnRedis" should {
    "send SpawnRedis message to agent and return a future with agent response" in {
      val randomPort = randomInt(10000)
      val prefix     = Prefix(s"esw.$randomString5")
      val args       = List("--port", randomPort.toString)
      withBehavior {
        case SpawnRedis(replyTo, `prefix`, `randomPort`, `args`) => replyTo ! Spawned
      } check { ac =>
        ac.spawnRedis(prefix, randomPort, args).futureValue should ===(Spawned)
      }
    }
  }

  "killComponent" should {
    "send KillComponent message to agent and return a future with agent response" in {
      val prefix      = Prefix(s"esw.$randomString5")
      val componentId = ComponentId(prefix, SequenceComponent)
      withBehavior {
        case KillComponent(replyTo, `componentId`) => replyTo ! Killed
      } check { ac =>
        ac.killComponent(componentId).futureValue should ===(Killed)
      }
    }
  }

  "getComponentStatus" should {
    "send GetComponentStatus message to agent and return a future with agent response" in {
      val prefix      = Prefix(s"esw.$randomString5")
      val componentId = ComponentId(prefix, SequenceComponent)
      withBehavior {
        case GetComponentStatus(replyTo, `componentId`) => replyTo ! Running
      } check { ac =>
        ac.getComponentStatus(componentId).futureValue should ===(Running)
      }
    }
  }

  "getAgentStatus" should {
    "send GetAgentStatus message to agent and return a future with agent response" in {
      val prefix      = Prefix(s"esw.$randomString5")
      val componentId = ComponentId(prefix, Service)
      withBehavior {
        case GetAgentStatus(replyTo) => replyTo ! AgentStatus(Map(ComponentId(prefix, Service) -> Stopping))
      } check { ac =>
        ac.getAgentStatus.futureValue should ===(AgentStatus(Map(componentId -> Stopping)))
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

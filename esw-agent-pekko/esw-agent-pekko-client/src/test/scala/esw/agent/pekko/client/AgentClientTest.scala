package esw.agent.pekko.client

import org.apache.pekko.actor.typed.ActorRef
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.models.ComponentType.{Machine, SequenceComponent}
import csw.location.api.models.Connection.PekkoConnection
import csw.location.api.models.{PekkoLocation, ComponentId, Metadata}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.pekko.client.AgentCommand.SpawnCommand.{SpawnSequenceComponent, SpawnSequenceManager}
import esw.agent.pekko.client.AgentCommand.{KillComponent, SpawnContainers}
import esw.agent.service.api.models.{Completed, KillResponse, SpawnResponse}
import esw.commons.utils.location.EswLocationError.{LocationNotFound, RegistrationListingFailed}
import esw.commons.utils.location.LocationServiceUtil
import esw.testcommons.{ActorTestSuit, AskProxyTestKit}

import java.net.URI
import java.nio.file.Path
import scala.concurrent.Future
import org.mockito.Mockito.when

class AgentClientTest extends ActorTestSuit {

  private val agentPrefix = Prefix(ESW, "agent")
  private val askProxyTestKit: AskProxyTestKit[AgentCommand, AgentClient] = new AskProxyTestKit[AgentCommand, AgentClient] {
    override def make(actorRef: ActorRef[AgentCommand]): AgentClient = {
      val location =
        PekkoLocation(
          PekkoConnection(ComponentId(agentPrefix, Machine)),
          actorRef.toURI,
          Metadata.empty
        )
      new AgentClient(location)
    }
  }

  import askProxyTestKit.*

  "make" should {
    "resolve the given prefix and return a new instance of AgentClient  | ESW-237" in {
      val locationService: LocationServiceUtil = mock[LocationServiceUtil]
      val prefix                               = Prefix("esw.test1")
      val pekkoConnection                      = PekkoConnection(ComponentId(prefix, Machine))
      val agentLocation                        = PekkoLocation(pekkoConnection, URI.create("pekko://abc"), Metadata.empty)
      when(locationService.find(pekkoConnection)).thenReturn(Future.successful(Right(agentLocation)))
      AgentClient.make(prefix, locationService).futureValue
    }

    "return a error when location service cant resolve agent with LocationNotFound | ESW-237" in {
      val locationService: LocationServiceUtil = mock[LocationServiceUtil]
      val prefix                               = Prefix("esw.test1")
      val pekkoConnection                      = PekkoConnection(ComponentId(prefix, Machine))
      val expectedError                        = LocationNotFound("error")
      when(locationService.find(pekkoConnection)).thenReturn(Future.successful(Left(expectedError)))

      AgentClient.make(prefix, locationService).leftValue should ===(expectedError)
    }

    "return a error when location service cant resolve agent with RegistrationListingFailedError  | ESW-237" in {
      val locationService: LocationServiceUtil = mock[LocationServiceUtil]
      val prefix                               = Prefix("esw.test1")
      val pekkoConnection                      = PekkoConnection(ComponentId(prefix, Machine))
      val expectedError                        = RegistrationListingFailed("error")
      when(locationService.find(pekkoConnection)).thenReturn(Future.successful(Left(expectedError)))

      AgentClient.make(prefix, locationService).leftValue should ===(expectedError)
    }

    "return a failed future when location service call fails  | ESW-237" in {
      val locationService: LocationServiceUtil = mock[LocationServiceUtil]
      val prefix                               = Prefix("esw.test1")
      val pekkoConnection                      = PekkoConnection(ComponentId(prefix, Machine))
      when(locationService.find(pekkoConnection)).thenReturn(Future.failed(new RuntimeException("boom")))
      val exception = intercept[RuntimeException](AgentClient.make(prefix, locationService).futureValue)
      exception.getCause.getMessage should ===(s"boom")
    }
  }

  "spawnSequenceComponent" should {
    "send SpawnSequenceComponent message to agent and return a future with agent response | ESW-362" in {
      val prefix        = Prefix(s"esw.$randomString5")
      val componentName = prefix.componentName
      val spawnResponse = mock[SpawnResponse]
      withBehavior { case SpawnSequenceComponent(replyTo, `agentPrefix`, `componentName`, None, _, _) =>
        replyTo ! spawnResponse
      } check { ac =>
        ac.spawnSequenceComponent(componentName, None, simulation = false, test = true).futureValue should ===(spawnResponse)
      }
    }

    "send SpawnSequenceComponent message with simulation flag set, to agent and return a future with agent response | ESW-362, ESW-174" in {
      val prefix        = Prefix(s"esw.$randomString5")
      val componentName = prefix.componentName
      val spawnResponse = mock[SpawnResponse]
      withBehavior { case SpawnSequenceComponent(replyTo, `agentPrefix`, `componentName`, None, _, _) =>
        replyTo ! spawnResponse
      } check { ac =>
        ac.spawnSequenceComponent(componentName, None, simulation = true, test = true).futureValue should ===(spawnResponse)
      }
    }
  }

  "killComponent" should {
    "send KillComponent message to agent and return a future with agent response | ESW-367, ESW-362" in {
      val location =
        PekkoLocation(PekkoConnection(ComponentId(Prefix("IRIS.filter"), SequenceComponent)), new URI("uri"), Metadata.empty)
      val killResponse = mock[KillResponse]
      withBehavior { case KillComponent(replyTo, `location`) =>
        replyTo ! killResponse
      } check { ac =>
        ac.killComponent(location).futureValue should ===(killResponse)
      }
    }
  }

  "spawnSequenceManager" should {
    "send spawnSequenceManager message to agent and return a future with agent response | ESW-180, ESW-362" in {
      val configPath    = Path.of("obsMode.conf")
      val spawnResponse = mock[SpawnResponse]
      withBehavior { case SpawnSequenceManager(replyTo, `configPath`, true, None, _) =>
        replyTo ! spawnResponse
      } check { ac =>
        ac.spawnSequenceManager(configPath, isConfigLocal = true).futureValue should ===(spawnResponse)
      }
    }

    "send spawnSequenceManager message with simulation flag set, to agent and return a future with agent response | ESW-180, ESW-362, ESW-174" in {
      val configPath    = Path.of("obsMode.conf")
      val spawnResponse = mock[SpawnResponse]
      withBehavior { case SpawnSequenceManager(replyTo, `configPath`, true, None, _) =>
        replyTo ! spawnResponse
      } check { ac =>
        ac.spawnSequenceManager(configPath, isConfigLocal = true, None, simulation = true).futureValue should ===(spawnResponse)
      }
    }
  }

  "spawnContainers" should {
    "send spawnSequenceManager message to agent and return a future with agent response | ESW-379" in {
      val hostConfigPath          = "hostConfig.conf"
      val spawnContainersResponse = mock[Completed]
      withBehavior { case SpawnContainers(replyTo, `hostConfigPath`, true) =>
        replyTo ! spawnContainersResponse
      } check { ac =>
        ac.spawnContainers(hostConfigPath, isConfigLocal = true).futureValue should ===(spawnContainersResponse)
      }
    }
  }
}

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
import esw.agent.akka.client.AgentCommand.SpawnCommand.{
  SpawnAAS,
  SpawnPostgres,
  SpawnRedis,
  SpawnSequenceComponent,
  SpawnSequenceManager
}
import esw.agent.service.api.models.{KillResponse, SpawnResponse}
import esw.commons.utils.location.EswLocationError.{LocationNotFound, RegistrationListingFailed}
import esw.commons.utils.location.LocationServiceUtil
import esw.constants.AgentConstants
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

    "return a error when location service cant resolve agent with LocationNotFound | ESW-237" in {
      val locationService: LocationServiceUtil = mock[LocationServiceUtil]
      val prefix                               = Prefix("esw.test1")
      val akkaConnection                       = AkkaConnection(ComponentId(prefix, Machine))
      val expectedError                        = LocationNotFound("error")
      when(locationService.find(akkaConnection)).thenReturn(Future.successful(Left(expectedError)))

      AgentClient.make(prefix, locationService).leftValue should ===(expectedError)
    }

    "return a error when location service cant resolve agent with RegistrationListingFailedError  | ESW-237" in {
      val locationService: LocationServiceUtil = mock[LocationServiceUtil]
      val prefix                               = Prefix("esw.test1")
      val akkaConnection                       = AkkaConnection(ComponentId(prefix, Machine))
      val expectedError                        = RegistrationListingFailed("error")
      when(locationService.find(akkaConnection)).thenReturn(Future.successful(Left(expectedError)))

      AgentClient.make(prefix, locationService).leftValue should ===(expectedError)
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
    "send SpawnSequenceComponent message to agent and return a future with agent response | ESW-362" in {
      val prefix        = Prefix(s"esw.$randomString5")
      val componentName = prefix.componentName
      val spawnResponse = mock[SpawnResponse]
      withBehavior {
        case SpawnSequenceComponent(replyTo, `agentPrefix`, `componentName`, None) => replyTo ! spawnResponse
      } check { ac =>
        ac.spawnSequenceComponent(componentName).futureValue should ===(spawnResponse)
      }
    }
  }

  "killComponent" should {
    "send KillComponent message to agent and return a future with agent response | ESW-367, ESW-362" in {
      val location =
        AkkaLocation(AkkaConnection(ComponentId(Prefix("IRIS.filter"), SequenceComponent)), new URI("uri"), Metadata.empty)
      val killResponse = mock[KillResponse]
      withBehavior {
        case KillComponent(replyTo, `location`) => replyTo ! killResponse
      } check { ac =>
        ac.killComponent(location).futureValue should ===(killResponse)
      }
    }
  }

  "spawnSequenceManager" should {
    "send spawnSequenceManager message to agent and return a future with agent response | ESW-180, ESW-362" in {
      val configPath    = Path.of("obsMode.conf")
      val spawnResponse = mock[SpawnResponse]
      withBehavior {
        case SpawnSequenceManager(replyTo, `configPath`, true, None) => replyTo ! spawnResponse
      } check { ac =>
        ac.spawnSequenceManager(configPath, isConfigLocal = true).futureValue should ===(spawnResponse)
      }
    }
  }

  "spawnEventServer" should {
    "send spawnRedis message to agent with the Event Server prefix and return a future with agent response | ESW-368" in {
      val configPath    = Path.of("redis-sentinel.conf")
      val spawnResponse = mock[SpawnResponse]
      val prefix        = AgentConstants.eventPrefix
      val port          = Some(8090)
      val version       = Some("0.1.0-SNAPSHOT")

      withBehavior {
        case SpawnRedis(replyTo, `prefix`, `configPath`, `port`, `version`) => replyTo ! spawnResponse
      } check { ac =>
        ac.spawnEventServer(configPath, port, version).futureValue should ===(spawnResponse)
      }
    }
  }

  "spawnAlarmServer" should {
    "send spawnRedis message to agent with the Alarm Server prefix and return a future with agent response | ESW-368" in {
      val configPath    = Path.of("redis-sentinel.conf")
      val spawnResponse = mock[SpawnResponse]
      val prefix        = AgentConstants.alarmPrefix
      val port          = Some(8090)
      val version       = Some("0.1.0-SNAPSHOT")

      withBehavior {
        case SpawnRedis(replyTo, `prefix`, `configPath`, `port`, `version`) => replyTo ! spawnResponse
      } check { ac =>
        ac.spawnAlarmServer(configPath, port, version).futureValue should ===(spawnResponse)
      }
    }
  }

  "spawnAAS" should {
    "send spawnAAS message to agent with the AAS server prefix and return a future with agent response | ESW-368" in {
      val migrationFilePath = Path.of("tmt-realm-file.json")
      val spawnResponse     = mock[SpawnResponse]
      val prefix            = AgentConstants.aasPrefix
      val port              = Some(8090)
      val version           = Some("0.1.0-SNAPSHOT")

      withBehavior {
        case SpawnAAS(replyTo, `prefix`, migrationFilePath, `port`, `version`) => replyTo ! spawnResponse
      } check { ac =>
        ac.spawnAAS(migrationFilePath, port, version).futureValue should ===(spawnResponse)
      }
    }
  }

  "spawnDatabaseServer" should {
    "send spawnPostgres message to agent with the Database Server prefix and return a future with agent response | ESW-368" in {
      val configPath       = Path.of("pg_hba.conf")
      val spawnResponse    = mock[SpawnResponse]
      val prefix           = AgentConstants.databasePrefix
      val dbUnixSocketDirs = "/tmp"
      val port             = Some(8090)
      val version          = Some("0.1.0-SNAPSHOT")

      withBehavior {
        case SpawnPostgres(replyTo, `prefix`, `configPath`, `port`, `dbUnixSocketDirs`, `version`) => replyTo ! spawnResponse
      } check { ac =>
        ac.spawnPostgres(configPath, port, dbUnixSocketDirs, version).futureValue should ===(spawnResponse)
      }
    }
  }
}

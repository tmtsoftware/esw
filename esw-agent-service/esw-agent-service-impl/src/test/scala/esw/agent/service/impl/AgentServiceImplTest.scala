package esw.agent.service.impl

import java.net.URI
import java.nio.file.Path

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.models.ComponentType.Machine
import csw.location.api.models.Connection.{AkkaConnection, TcpConnection}
import csw.location.api.models.{AkkaLocation, ComponentId, Metadata, TcpLocation}
import csw.prefix.models.Prefix
import esw.agent.akka.client.AgentClient
import esw.agent.service.api.models.{Failed, Killed, SpawnResponse}
import esw.commons.utils.location.EswLocationError.LocationNotFound
import esw.commons.utils.location.LocationServiceUtil
import esw.testcommons.BaseTestSuite

import scala.concurrent.Future

class AgentServiceImplTest extends BaseTestSuite {

  private implicit val testSystem: ActorSystem[_] = ActorSystem(SpawnProtocol(), "test")
  private val locationService                     = mock[LocationServiceUtil]
  private val agentClientMock                     = mock[AgentClient]
  private val agentPrefix                         = mock[Prefix]
  private val version                             = Some(randomString(10))

  private val agentService = new AgentServiceImpl(locationService) {
    override private[impl] def agentClient(agentPrefix: Prefix): Future[Either[String, AgentClient]] =
      Future.successful(Right(agentClientMock))
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    testSystem.terminate()
  }

  "AgentService" must {

    "spawnSequenceComponent Api" must {

      val spawnRes      = mock[SpawnResponse]
      val componentName = randomString(10)

      "be able to send spawn sequence component message to given agent" in {
        when(agentClientMock.spawnSequenceComponent(componentName, version)).thenReturn(Future.successful(spawnRes))
        agentService.spawnSequenceComponent(agentPrefix, componentName, version).futureValue
        verify(agentClientMock).spawnSequenceComponent(componentName, version)
      }

      "give Failed when agent is not there | ESW-361" in {
        val locationService  = mock[LocationServiceUtil]
        val akkaConnection   = AkkaConnection(ComponentId(agentPrefix, Machine))
        val expectedErrorMsg = "error"

        when(locationService.find(akkaConnection)).thenReturn(Future.successful(Left(LocationNotFound(expectedErrorMsg))))

        val agentService = new AgentServiceImpl(locationService)

        agentService.spawnSequenceComponent(agentPrefix, componentName, version).futureValue should ===(Failed(expectedErrorMsg))

        verify(locationService).find(akkaConnection)
      }
    }

    "spawnSequenceManager Api" must {
      val obsConfPath = mock[Path]

      "be able to send spawn sequence manager message to given agent | ESW-361" in {

        val spawnRes = mock[SpawnResponse]
        when(agentClientMock.spawnSequenceManager(obsConfPath, isConfigLocal = true, version))
          .thenReturn(Future.successful(spawnRes))

        agentService.spawnSequenceManager(agentPrefix, obsConfPath, isConfigLocal = true, version).futureValue

        verify(agentClientMock).spawnSequenceManager(obsConfPath, isConfigLocal = true, version)
      }

      "give Failed when agent is not there | ESW-361" in {
        val locationService = mock[LocationServiceUtil]

        val akkaConnection   = AkkaConnection(ComponentId(agentPrefix, Machine))
        val expectedErrorMsg = "error"
        when(locationService.find(akkaConnection)).thenReturn(Future.successful(Left(LocationNotFound(expectedErrorMsg))))

        val agentService = new AgentServiceImpl(locationService)

        agentService.spawnSequenceManager(agentPrefix, obsConfPath, isConfigLocal = true, version).futureValue should ===(
          Failed(expectedErrorMsg)
        )

        verify(locationService).find(akkaConnection)
      }
    }

    "killComponent Api" must {
      val componentId     = mock[ComponentId]
      val componentPrefix = mock[Prefix]
      when(componentId.prefix).thenReturn(componentPrefix)

      val componentConnection = AkkaConnection(componentId)
      val agentPrefixStr      = "IRIS.filterWheel"
      val agentPrefix         = Prefix(agentPrefixStr)
      val componentLocation   = AkkaLocation(componentConnection, new URI("xyz"), Metadata().withAgentPrefix(agentPrefix))

      "be able to kill component for the given componentId | ESW-361, ESW-367" in {
        when(agentClientMock.killComponent(componentLocation)).thenReturn(Future.successful(Killed))
        when(locationService.list(componentId)).thenReturn(Future.successful(List(componentLocation)))
        agentService.killComponent(componentId).futureValue

        verify(locationService).list(componentId)
        verify(agentClientMock).killComponent(componentLocation)
      }

      "give error message when agent is not there| ESW-361" in {
        val agentConnection  = AkkaConnection(ComponentId(agentPrefix, Machine))
        val expectedErrorMsg = "error"

        when(locationService.list(componentId)).thenReturn(Future.successful(List(componentLocation)))
        when(locationService.find(agentConnection)).thenReturn(Future.successful(Left(LocationNotFound(expectedErrorMsg))))

        val agentService = new AgentServiceImpl(locationService)
        agentService.killComponent(componentId).futureValue should ===(Failed(expectedErrorMsg))

        verify(locationService).find(agentConnection)
      }

      "be able to return an error if component location does not contain agent prefix | ESW-361, ESW-367" in {
        val compLocWithoutAgentPrefix = AkkaLocation(componentConnection, new URI("xyz"), Metadata.empty)
        when(locationService.list(componentId)).thenReturn(Future.successful(List(compLocWithoutAgentPrefix)))

        agentService.killComponent(componentId).futureValue should ===(
          Failed(s"$compLocWithoutAgentPrefix metadata does not contain agent prefix")
        )
      }

      "be able to return an error if component locations do not contain agent prefix | ESW-361, ESW-367" in {
        val compLocWithoutAgentPrefix = AkkaLocation(componentConnection, new URI("xyz"), Metadata.empty)

        val componentTcpConnection       = TcpConnection(componentId)
        val compLocWithoutAgentPrefixTwo = TcpLocation(componentTcpConnection, new URI("xyz"), Metadata.empty)

        when(locationService.list(componentId))
          .thenReturn(Future.successful(List(compLocWithoutAgentPrefix, compLocWithoutAgentPrefixTwo)))

        println(agentService.killComponent(componentId).futureValue)
        agentService.killComponent(componentId).futureValue should ===(
          Failed(
            s"$compLocWithoutAgentPrefix metadata does not contain agent prefix," +
              s"$compLocWithoutAgentPrefixTwo metadata does not contain agent prefix"
          )
        )
      }
    }

    "agentClient Api" must {
      "be able to create agent client for given agentPrefix | ESW-361" in {
        val locationService = mock[LocationServiceUtil]
        val akkaConnection  = AkkaConnection(ComponentId(agentPrefix, Machine))
        val location        = AkkaLocation(akkaConnection, URI.create("some"), Metadata.empty)
        when(locationService.find(akkaConnection)).thenReturn(Future.successful(Right(location)))

        val agentService = new AgentServiceImpl(locationService)
        agentService.agentClient(agentPrefix).futureValue
        verify(locationService).find(akkaConnection)
      }

      "be able to throw AgentNotFound exception for given agentPrefix | ESW-361" in {
        val locationService = mock[LocationServiceUtil]

        val akkaConnection = AkkaConnection(ComponentId(agentPrefix, Machine))
        when(locationService.find(akkaConnection)).thenReturn(Future.successful(Left(LocationNotFound("error"))))

        val agentService = new AgentServiceImpl(locationService)

        agentService.agentClient(agentPrefix).leftValue should ===("error")

        verify(locationService).find(akkaConnection)
      }
    }

  }
}

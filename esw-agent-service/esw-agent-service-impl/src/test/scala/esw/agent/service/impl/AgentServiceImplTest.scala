package esw.agent.service.impl

import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.models.ComponentType.Machine
import csw.location.api.models.Connection.PekkoConnection
import csw.location.api.models.{PekkoLocation, ComponentId, Metadata}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.pekko.client.AgentClient
import esw.agent.service.api.models.*
import esw.commons.utils.location.EswLocationError.*
import esw.commons.utils.location.LocationServiceUtil
import esw.testcommons.BaseTestSuite
import org.mockito.Mockito.{reset, verify, when}

import java.net.URI
import java.nio.file.Path
import scala.concurrent.Future

class AgentServiceImplTest extends BaseTestSuite {

  private implicit val testSystem: ActorSystem[?] = ActorSystem(SpawnProtocol(), "test")
  private val locationService                     = mock[LocationServiceUtil]
  private val agentStatusUtil                     = mock[AgentStatusUtil]
  private val agentClientMock                     = mock[AgentClient]
  private val agentPrefix                         = mock[Prefix]
  private val version                             = Some(randomString(10))

  private val agentService = new AgentServiceImpl(locationService, agentStatusUtil) {
    override private[impl] def agentClient(agentPrefix: Prefix): Future[Either[String, AgentClient]] =
      Future.successful(Right(agentClientMock))

    override private[impl] def makeAgentClient(pekkoLocation: PekkoLocation) = agentClientMock
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(locationService)
    reset(agentStatusUtil)
    reset(agentClientMock)
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    testSystem.terminate()
  }

  "AgentService" must {

    "spawnSequenceComponent API" must {

      val spawnRes      = mock[SpawnResponse]
      val componentName = randomString(10)

      "be able to send spawn sequence component message to given agent" in {
        when(agentClientMock.spawnSequenceComponent(componentName, version, false, false)).thenReturn(Future.successful(spawnRes))
        agentService.spawnSequenceComponent(agentPrefix, componentName, version).futureValue
        verify(agentClientMock).spawnSequenceComponent(componentName, version, false, false)
      }

      "give Failed when agent is not there | ESW-361" in {
        val locationService  = mock[LocationServiceUtil]
        val pekkoConnection  = PekkoConnection(ComponentId(agentPrefix, Machine))
        val expectedErrorMsg = "error"

        when(locationService.find(pekkoConnection)).thenReturn(Future.successful(Left(LocationNotFound(expectedErrorMsg))))

        val agentService = new AgentServiceImpl(locationService, agentStatusUtil)

        agentService.spawnSequenceComponent(agentPrefix, componentName, version).futureValue should ===(Failed(expectedErrorMsg))

        verify(locationService).find(pekkoConnection)
      }
    }

    "spawnSequenceManager API" must {
      val obsConfPath = mock[Path]

      "be able to send spawn sequence manager message to given agent | ESW-361" in {

        val spawnRes = mock[SpawnResponse]
        when(agentClientMock.spawnSequenceManager(obsConfPath, true, version, false))
          .thenReturn(Future.successful(spawnRes))

        agentService.spawnSequenceManager(agentPrefix, obsConfPath, isConfigLocal = true, version).futureValue

        verify(agentClientMock).spawnSequenceManager(obsConfPath, isConfigLocal = true, version)
      }

      "give Failed when agent is not there | ESW-361" in {
        val locationService = mock[LocationServiceUtil]

        val pekkoConnection  = PekkoConnection(ComponentId(agentPrefix, Machine))
        val expectedErrorMsg = "error"
        when(locationService.find(pekkoConnection)).thenReturn(Future.successful(Left(LocationNotFound(expectedErrorMsg))))

        val agentService = new AgentServiceImpl(locationService, agentStatusUtil)

        agentService.spawnSequenceManager(agentPrefix, obsConfPath, isConfigLocal = true, version).futureValue should ===(
          Failed(expectedErrorMsg)
        )

        verify(locationService).find(pekkoConnection)
      }
    }

    "spawnContainer API" must {
      val agentPrefix    = mock[Prefix]
      val hostConfigPath = randomString(5)
      val isConfigLocal  = randomBool

      "be able to spawn containers for given hostConfig | ESW-480" in {
        when(agentClientMock.spawnContainers(hostConfigPath, isConfigLocal))
          .thenReturn(Future.successful(Completed(Map.empty)))

        agentService.spawnContainers(agentPrefix, hostConfigPath, isConfigLocal).futureValue

        verify(agentClientMock).spawnContainers(hostConfigPath, isConfigLocal)
      }

      "give Failed when agent is not available | ESW-480" in {
        val locationService = mock[LocationServiceUtil]

        val pekkoConnection  = PekkoConnection(ComponentId(agentPrefix, Machine))
        val expectedErrorMsg = "error"
        when(locationService.find(pekkoConnection)).thenReturn(Future.successful(Left(LocationNotFound(expectedErrorMsg))))

        val agentService = new AgentServiceImpl(locationService, agentStatusUtil)

        agentService.spawnContainers(agentPrefix, hostConfigPath, isConfigLocal).futureValue should ===(
          Failed(expectedErrorMsg)
        )

        verify(locationService).find(pekkoConnection)
      }
    }

    "killComponent API" must {
      val componentId     = mock[ComponentId]
      val componentPrefix = mock[Prefix]
      val hostname        = "192.168.1.3"
      when(componentId.prefix).thenReturn(componentPrefix)
      when(componentId.componentType).thenReturn(Machine)

      val componentConnection = PekkoConnection(componentId)
      val componentLocation = PekkoLocation(
        componentConnection,
        new URI(s"pekko://iris-system@$hostname:59405/user/iris#663907945"),
        Metadata.empty
      )
      val agentLocation = PekkoLocation(componentConnection, new URI(hostname), Metadata.empty)

      "be able to kill component for the given componentId | ESW-361, ESW-367, ESW-480" in {
        when(agentClientMock.killComponent(componentLocation)).thenReturn(Future.successful(Killed))
        when(locationService.findPekkoLocation(componentId.prefix.toString(), componentId.componentType))
          .thenReturn(Future.successful(Right(componentLocation)))
        when(locationService.findAgentByHostname(hostname)).thenReturn(Future.successful(Right(agentLocation)))

        agentService.killComponent(componentId).futureValue should ===(Killed)

        verify(locationService).findPekkoLocation(componentId.prefix.toString(), componentId.componentType)
        verify(agentClientMock).killComponent(componentLocation)
      }

      "give error message when agent is not there | ESW-361, ESW-480" in {
        val expectedErrorMsg = "error"

        when(locationService.findPekkoLocation(componentId.prefix.toString(), componentId.componentType))
          .thenReturn(Future.successful(Right(componentLocation)))
        when(locationService.findAgentByHostname(hostname))
          .thenReturn(Future.successful(Left(LocationNotFound(expectedErrorMsg))))

        val agentService = new AgentServiceImpl(locationService, agentStatusUtil)
        agentService.killComponent(componentId).futureValue should ===(Failed(expectedErrorMsg))

        verify(locationService).findPekkoLocation(componentId.prefix.toString(), componentId.componentType)
      }

      "be able to return an error if component is not there | ESW-361, ESW-367, ESW-480" in {
        val expectedErrorMsg = "error"
        when(locationService.findPekkoLocation(componentId.prefix.toString(), componentId.componentType))
          .thenReturn(Future.successful(Left(LocationNotFound(expectedErrorMsg))))

        agentService.killComponent(componentId).futureValue should ===(Failed(expectedErrorMsg))

        verify(locationService).findPekkoLocation(componentId.prefix.toString(), componentId.componentType)
      }
    }

    "agentClient API" must {
      "be able to create agent client for given agentPrefix | ESW-361" in {
        val locationService = mock[LocationServiceUtil]
        val pekkoConnection = PekkoConnection(ComponentId(agentPrefix, Machine))
        val location        = PekkoLocation(pekkoConnection, URI.create("some"), Metadata.empty)
        when(locationService.find(pekkoConnection)).thenReturn(Future.successful(Right(location)))

        val agentService = new AgentServiceImpl(locationService, agentStatusUtil)
        agentService.agentClient(agentPrefix).futureValue
        verify(locationService).find(pekkoConnection)
      }

      "be able to throw AgentNotFound exception for given agentPrefix | ESW-361" in {
        val locationService = mock[LocationServiceUtil]

        val pekkoConnection = PekkoConnection(ComponentId(agentPrefix, Machine))
        when(locationService.find(pekkoConnection)).thenReturn(Future.successful(Left(LocationNotFound("error"))))

        val agentService = new AgentServiceImpl(locationService, agentStatusUtil)

        agentService.agentClient(agentPrefix).leftValue should ===("error")

        verify(locationService).find(pekkoConnection)
      }
    }

    "getAgentStatus API" must {
      "be able to get agent status" in {
        val agentStatusResponse = AgentStatusResponse.Success(
          List(AgentStatus(ComponentId(Prefix(ESW, "agent1"), Machine), List.empty[SequenceComponentStatus])),
          List.empty
        )

        when(agentStatusUtil.getAllAgentStatus).thenReturn(Future.successful(agentStatusResponse))

        val response = agentService.getAgentStatus.futureValue

        response should ===(agentStatusResponse)
        verify(agentStatusUtil).getAllAgentStatus
      }
    }
  }
}

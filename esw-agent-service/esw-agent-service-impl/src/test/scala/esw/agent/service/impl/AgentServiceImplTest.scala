package esw.agent.service.impl

import java.net.URI
import java.nio.file.Path

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.models.ComponentType.Machine
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, Metadata}
import csw.location.api.scaladsl.LocationService
import csw.prefix.models.{Prefix, Subsystem}
import esw.agent.akka.client.AgentClient
import esw.agent.service.api.models.{Failed, Killed, SpawnResponse}
import esw.testcommons.BaseTestSuite

import scala.concurrent.Future

class AgentServiceImplTest extends BaseTestSuite {

  private implicit val testSystem: ActorSystem[_] = ActorSystem(SpawnProtocol(), "test")
  private val locationService                     = mock[LocationService]
  private val agentClientMock                     = mock[AgentClient]
  private val agentPrefix                         = mock[Prefix]
  private val version                             = Some(randomString(10))

  private val agentService = new AgentServiceImpl(locationService) {
    override private[impl] def agentClient(agentPrefix: Prefix): Future[Either[Failed, AgentClient]] =
      Future.successful(Right(agentClientMock))
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    testSystem.terminate()
  }

  "AgentService" must {

    "spawnSequenceComponent Api" must {

      val subsystem     = mock[Subsystem]
      val spawnRes      = mock[SpawnResponse]
      val componentName = randomString(10)

      "be able to send spawn sequence component message to given agent" in {

        when(agentClientMock.spawnSequenceComponent(componentName, version)).thenReturn(Future.successful(spawnRes))
        when(agentPrefix.subsystem).thenReturn(subsystem)

        agentService.spawnSequenceComponent(agentPrefix, componentName, version).futureValue

        verify(agentClientMock).spawnSequenceComponent(componentName, version)
      }

      "give Failed when agent is not there | ESW-361" in {
        val locationService = mock[LocationService]

        val akkaConnection = AkkaConnection(ComponentId(agentPrefix, Machine))
        when(locationService.find(akkaConnection)).thenReturn(Future.successful(None))

        val agentService = new AgentServiceImpl(locationService)

        agentService.spawnSequenceComponent(agentPrefix, componentName, version).futureValue should ===(
          Failed(s"could not resolve agent with prefix: $agentPrefix")
        )

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
        val locationService = mock[LocationService]

        val akkaConnection = AkkaConnection(ComponentId(agentPrefix, Machine))
        when(locationService.find(akkaConnection)).thenReturn(Future.successful(None))

        val agentService = new AgentServiceImpl(locationService)

        agentService.spawnSequenceManager(agentPrefix, obsConfPath, isConfigLocal = true, version).futureValue should ===(
          Failed(s"could not resolve agent with prefix: $agentPrefix")
        )

        verify(locationService).find(akkaConnection)
      }
    }

    "killComponent Api" must {

      val componentId = mock[ComponentId]
      "be able to kill component for the given componentId | ESW-361" in {
        when(agentClientMock.killComponent(componentId)).thenReturn(Future.successful(Killed))
        agentService.killComponent(agentPrefix, componentId).futureValue

        verify(agentClientMock).killComponent(componentId)
      }

      "give Failed when agent is not there| ESW-361" in {
        val locationService = mock[LocationService]

        val akkaConnection = AkkaConnection(ComponentId(agentPrefix, Machine))
        when(locationService.find(akkaConnection)).thenReturn(Future.successful(None))

        val agentService = new AgentServiceImpl(locationService)

        agentService.killComponent(agentPrefix, componentId).futureValue should ===(
          Failed(s"could not resolve agent with prefix: $agentPrefix")
        )

        verify(locationService).find(akkaConnection)
      }
    }

    "agentClient Api" must {
      "be able to create agent client for given agentPrefix | ESW-361" in {
        val locationService = mock[LocationService]

        val akkaConnection = AkkaConnection(ComponentId(agentPrefix, Machine))
        val location       = AkkaLocation(akkaConnection, URI.create("some"), Metadata.empty)
        when(locationService.find(akkaConnection)).thenReturn(Future.successful(Some(location)))

        val agentService = new AgentServiceImpl(locationService)

        agentService.agentClient(agentPrefix).futureValue

        verify(locationService).find(akkaConnection)
      }

      "be able to throw AgentNotFound exception for given agentPrefix | ESW-361" in {
        val locationService = mock[LocationService]

        val akkaConnection = AkkaConnection(ComponentId(agentPrefix, Machine))
        when(locationService.find(akkaConnection)).thenReturn(Future.successful(None))

        val agentService = new AgentServiceImpl(locationService)

        agentService.agentClient(agentPrefix).futureValue should ===(
          Left(Failed(s"could not resolve agent with prefix: $agentPrefix"))
        )

        verify(locationService).find(akkaConnection)
      }
    }

  }
}

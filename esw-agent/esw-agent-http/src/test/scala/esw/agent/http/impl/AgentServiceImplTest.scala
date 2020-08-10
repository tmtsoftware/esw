package esw.agent.http.impl

import java.net.URI
import java.nio.file.Path

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.models.ComponentType.Machine
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, Metadata}
import csw.location.api.scaladsl.LocationService
import csw.prefix.models.Prefix
import esw.agent.api.{AgentNotFoundException, SpawnResponse}
import esw.agent.client.AgentClient
import esw.testcommons.BaseTestSuite

import scala.concurrent.Future

class AgentServiceImplTest extends BaseTestSuite {

  private implicit val testSystem: ActorSystem[_] = ActorSystem(SpawnProtocol(), "test")
  private val locationService                     = mock[LocationService]
  private val agentClientMock                     = mock[AgentClient]
  private val agentPrefix                         = mock[Prefix]

  private val agentService = new AgentServiceImpl(locationService) {
    override private[impl] def agentClient(agentPrefix: Prefix): Future[AgentClient] = Future.successful(agentClientMock)
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    testSystem.terminate()
  }

  "AgentService" must {
    "be able to send spawn sequence component message to given agent" in {
      val seqCompPrefix = mock[Prefix]

      val spawnRes = mock[SpawnResponse]
      when(agentClientMock.spawnSequenceComponent(seqCompPrefix)).thenReturn(Future.successful(spawnRes))

      agentService.spawnSequenceComponent(agentPrefix, seqCompPrefix)

      verify(agentClientMock).spawnSequenceComponent(seqCompPrefix, None)
    }

    "be able to send spawn sequence manager message to given agent | ESW-361" in {
      val obsConfPath = mock[Path]

      val spawnRes = mock[SpawnResponse]
      when(agentClientMock.spawnSequenceManager(obsConfPath, isConfigLocal = true, None)).thenReturn(Future.successful(spawnRes))

      agentService.spawnSequenceManager(agentPrefix, obsConfPath, isConfigLocal = true, None)

      verify(agentClientMock).spawnSequenceManager(obsConfPath, isConfigLocal = true, None)
    }

    "be able to create agent client for given agentPrefix | ESW-361" in {
      val locationService = mock[LocationService]

      val akkaConnection = AkkaConnection(ComponentId(agentPrefix, Machine))
      val location       = AkkaLocation(akkaConnection, URI.create("some"), Metadata.empty)
      when(locationService.find(akkaConnection)).thenReturn(Future.successful(Some(location)))

      val agentService = new AgentServiceImpl(locationService)

      agentService.agentClient(agentPrefix).futureValue

      verify(locationService).find(akkaConnection)
    }

    "be throw AgentNotFound exception for given agentPrefix | ESW-361" in {
      val locationService = mock[LocationService]

      val akkaConnection = AkkaConnection(ComponentId(agentPrefix, Machine))
      when(locationService.find(akkaConnection)).thenReturn(Future.successful(None))

      val agentService = new AgentServiceImpl(locationService)

      agentService.agentClient(agentPrefix).failed.futureValue should ===(
        AgentNotFoundException(s"could not resolve agent with prefix: $agentPrefix")
      )

      verify(locationService).find(akkaConnection)
    }
  }
}

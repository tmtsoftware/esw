package esw.agent.http.impl

import java.nio.file.Path

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.scaladsl.LocationService
import csw.prefix.models.Prefix
import esw.agent.api.SpawnResponse
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
  }
}

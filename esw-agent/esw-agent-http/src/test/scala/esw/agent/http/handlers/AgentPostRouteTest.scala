package esw.agent.http.handlers

import java.nio.file.Path

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.testkit.ScalatestRouteTest
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, TCS}
import esw.agent.api.codecs.AgentHttpCodecs
import esw.agent.api.protocol.AgentPostRequest
import esw.agent.api.protocol.AgentPostRequest.{SpawnSequenceComponent, SpawnSequenceManager}
import esw.agent.api.{AgentNotFoundException, SpawnResponse, Spawned}
import esw.agent.http.api.AgentService
import esw.testcommons.BaseTestSuite
import msocket.api.ContentType
import msocket.impl.post.{ClientHttpCodecs, PostRouteFactory}

import scala.concurrent.Future

class AgentPostRouteTest extends BaseTestSuite with ScalatestRouteTest with AgentHttpCodecs with ClientHttpCodecs {

  def clientContentType: ContentType = ContentType.Json

  private val agentService: AgentService = mock[AgentService]

  private val route = new PostRouteFactory("post-endpoint", new AgentPostHandlerImpl(agentService)).make()

  private def post(entity: AgentPostRequest): HttpRequest = Post("/post-endpoint", entity)

  "SpawnSequenceManager" must {
    "be able to start a sequence manager | ESW-361" in {
      val agentPrefix = Prefix(ESW, "Agent_1")
      val obsConfPath = Path.of("/obsConf")

      val spawnSMRequest = SpawnSequenceManager(agentPrefix, obsConfPath, isConfigLocal = true, None)

      when(agentService.spawnSequenceManager(agentPrefix, obsConfPath, isConfigLocal = true, None))
        .thenReturn(Future.successful(Spawned))

      post(spawnSMRequest) ~> route ~> check {
        responseAs[SpawnResponse] should ===(Spawned)
      }
    }

    "be able to send failure response when agent is not found | ESW-361" in {
      val agentPrefix = Prefix(ESW, "Agent_1")
      val obsConfPath = Path.of("/obsConf")

      val spawnSMRequest = SpawnSequenceManager(agentPrefix, obsConfPath, isConfigLocal = true, None)

      when(agentService.spawnSequenceManager(agentPrefix, obsConfPath, isConfigLocal = true, None))
        .thenReturn(Future.failed(AgentNotFoundException("Exception")))

      post(spawnSMRequest) ~> route ~> check {
        responseAs[AgentNotFoundException] should ===(AgentNotFoundException("Exception"))
      }
    }
  }

  "SpawnSequenceComponent" must {
    "be able to start a sequence component | ESW-361" in {
      val agentPrefix   = Prefix(ESW, "Agent_1")
      val seqCompPrefix = Prefix(TCS, "TCS_1")

      val spawnSeqCompRequest = SpawnSequenceComponent(agentPrefix, seqCompPrefix, None)

      when(agentService.spawnSequenceComponent(agentPrefix, seqCompPrefix, None))
        .thenReturn(Future.successful(Spawned))

      post(spawnSeqCompRequest) ~> route ~> check {
        responseAs[SpawnResponse] should ===(Spawned)
      }
    }

    "be able to send failure response when agent is not found | ESW-361" in {
      val agentPrefix   = Prefix(ESW, "Agent_1")
      val seqCompPrefix = Prefix(TCS, "TCS_1")

      val spawnSeqCompRequest = SpawnSequenceComponent(agentPrefix, seqCompPrefix, None)

      when(agentService.spawnSequenceComponent(agentPrefix, seqCompPrefix, None))
        .thenReturn(Future.failed(AgentNotFoundException("Exception")))

      post(spawnSeqCompRequest) ~> route ~> check {
        responseAs[AgentNotFoundException] should ===(AgentNotFoundException("Exception"))
      }
    }
  }
}

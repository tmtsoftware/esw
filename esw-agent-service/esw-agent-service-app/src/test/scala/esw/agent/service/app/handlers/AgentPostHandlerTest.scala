package esw.agent.service.app.handlers

import java.nio.file.Path

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.directives.BasicDirectives
import akka.http.scaladsl.testkit.ScalatestRouteTest
import csw.aas.core.token.AccessToken
import csw.aas.http.SecurityDirectives
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Service
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.service.api.AgentService
import esw.agent.service.api.codecs.AgentHttpCodecs
import esw.agent.service.api.models._
import esw.agent.service.api.protocol.AgentPostRequest
import esw.agent.service.api.protocol.AgentPostRequest.{SpawnSequenceComponent, SpawnSequenceManager, StopComponent}
import esw.agent.service.app.auth.EswUserRolePolicy
import esw.testcommons.BaseTestSuite
import msocket.api.ContentType
import msocket.impl.post.{ClientHttpCodecs, PostRouteFactory}

import scala.concurrent.Future

class AgentPostHandlerTest extends BaseTestSuite with ScalatestRouteTest with AgentHttpCodecs with ClientHttpCodecs {

  def clientContentType: ContentType = ContentType.Json

  private val agentService: AgentService = mock[AgentService]
  private val securityDirective          = mock[SecurityDirectives]

  private val route = new PostRouteFactory("post-endpoint", new AgentPostHandlerImpl(agentService, securityDirective)).make()

  private def post(entity: AgentPostRequest): HttpRequest = Post("/post-endpoint", entity)

  private val agentPrefix: Prefix = Prefix(ESW, "Agent_1")
  private val dummyDirective      = BasicDirectives.extract[AccessToken](_ => AccessToken())

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(securityDirective, agentService)
  }

  "SpawnSequenceManager" must {
    "be able to start a sequence manager | ESW-361" in {
      val obsConfPath    = Path.of("/obsConf")
      val spawnSMRequest = SpawnSequenceManager(agentPrefix, obsConfPath, isConfigLocal = true, None)

      when(securityDirective.sPost(EswUserRolePolicy())).thenReturn(dummyDirective)
      when(agentService.spawnSequenceManager(agentPrefix, obsConfPath, isConfigLocal = true, None))
        .thenReturn(Future.successful(Spawned))

      post(spawnSMRequest) ~> route ~> check {
        verify(securityDirective).sPost(EswUserRolePolicy())
        responseAs[SpawnResponse] should ===(Spawned)
      }
    }

    "be able to send failure response when agent is not found | ESW-361" in {
      val obsConfPath = Path.of("/obsConf")

      val spawnSMRequest = SpawnSequenceManager(agentPrefix, obsConfPath, isConfigLocal = true, None)

      when(securityDirective.sPost(EswUserRolePolicy())).thenReturn(dummyDirective)
      when(agentService.spawnSequenceManager(agentPrefix, obsConfPath, isConfigLocal = true, None))
        .thenReturn(Future.failed(AgentNotFoundException("Exception")))

      post(spawnSMRequest) ~> route ~> check {
        verify(securityDirective).sPost(EswUserRolePolicy())
        responseAs[AgentNotFoundException] should ===(AgentNotFoundException("Exception"))
      }
    }
  }

  "SpawnSequenceComponent" must {

    "be able to start a sequence component | ESW-361" in {
      val spawnSeqCompRequest = SpawnSequenceComponent(agentPrefix, "TCS_1", None)

      when(securityDirective.sPost(EswUserRolePolicy())).thenReturn(dummyDirective)
      when(agentService.spawnSequenceComponent(agentPrefix, "TCS_1", None))
        .thenReturn(Future.successful(Spawned))

      post(spawnSeqCompRequest) ~> route ~> check {
        verify(securityDirective).sPost(EswUserRolePolicy())
        responseAs[SpawnResponse] should ===(Spawned)
      }
    }

    "be able to send failure response when agent is not found | ESW-361" in {
      val spawnSeqCompRequest = SpawnSequenceComponent(agentPrefix, "TCS_1", None)

      when(securityDirective.sPost(EswUserRolePolicy())).thenReturn(dummyDirective)
      when(agentService.spawnSequenceComponent(agentPrefix, "TCS_1", None))
        .thenReturn(Future.failed(AgentNotFoundException("Exception")))

      post(spawnSeqCompRequest) ~> route ~> check {
        verify(securityDirective).sPost(EswUserRolePolicy())
        responseAs[AgentNotFoundException] should ===(AgentNotFoundException("Exception"))
      }
    }

  }

  "StopComponent" must {
    "be able to stop component of the given componentId | ESW-361" in {

      val componentId          = ComponentId(Prefix(ESW, "sequence_manager"), Service)
      val stopComponentRequest = StopComponent(agentPrefix, componentId)

      when(securityDirective.sPost(EswUserRolePolicy())).thenReturn(dummyDirective)
      when(agentService.stopComponent(agentPrefix, componentId)).thenReturn(Future.successful(Killed))

      post(stopComponentRequest) ~> route ~> check {
        verify(securityDirective).sPost(EswUserRolePolicy())
        responseAs[KillResponse] should ===(Killed)
      }
    }
  }
}

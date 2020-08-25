package esw.agent.service.app.handlers

import java.nio.file.Path

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.directives.BasicDirectives
import akka.http.scaladsl.testkit.ScalatestRouteTest
import csw.aas.core.token.AccessToken
import csw.aas.http.SecurityDirectives
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Service
import csw.location.api.models.Connection.AkkaConnection
import csw.prefix.models.Prefix
import esw.agent.service.api.AgentServiceApi
import esw.agent.service.api.codecs.AgentServiceCodecs
import esw.agent.service.api.models._
import esw.agent.service.api.protocol.AgentServiceRequest
import esw.agent.service.api.protocol.AgentServiceRequest.{KillComponent, SpawnSequenceComponent, SpawnSequenceManager}
import esw.agent.service.app.auth.EswUserRolePolicy
import esw.testcommons.BaseTestSuite
import msocket.api.ContentType
import msocket.impl.post.{ClientHttpCodecs, PostRouteFactory}

import scala.concurrent.Future

class AgentPostHandlerTest extends BaseTestSuite with ScalatestRouteTest with AgentServiceCodecs with ClientHttpCodecs {

  def clientContentType: ContentType = ContentType.Json

  private val agentService: AgentServiceApi = mock[AgentServiceApi]
  private val securityDirective             = mock[SecurityDirectives]

  private val route =
    new PostRouteFactory("post-endpoint", new AgentServicePostHandler(agentService, securityDirective)).make()

  private def post(entity: AgentServiceRequest): HttpRequest = Post("/post-endpoint", entity)

  private val sequenceCompName       = randomString(10)
  private val dummyDirective         = BasicDirectives.extract[AccessToken](_ => AccessToken())
  private val agentPrefix: Prefix    = Prefix(randomSubsystem, randomString(10))
  private val failedResponse: Failed = Failed(randomString(20))

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(securityDirective, agentService)
  }

  "SpawnSequenceManager" must {
    val obsConfPath = Path.of(randomString(5))

    "be able to start a sequence manager | ESW-361" in {
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
      val spawnSMRequest = SpawnSequenceManager(agentPrefix, obsConfPath, isConfigLocal = false, None)

      when(securityDirective.sPost(EswUserRolePolicy())).thenReturn(dummyDirective)
      when(agentService.spawnSequenceManager(agentPrefix, obsConfPath, isConfigLocal = false, None))
        .thenReturn(Future.successful(failedResponse))

      post(spawnSMRequest) ~> route ~> check {
        verify(securityDirective).sPost(EswUserRolePolicy())
        responseAs[SpawnResponse] should ===(failedResponse)
      }
    }
  }

  "SpawnSequenceComponent" must {

    "be able to start a sequence component | ESW-361" in {
      val spawnSeqCompRequest = SpawnSequenceComponent(agentPrefix, sequenceCompName, None)

      when(securityDirective.sPost(EswUserRolePolicy())).thenReturn(dummyDirective)
      when(agentService.spawnSequenceComponent(agentPrefix, sequenceCompName, None))
        .thenReturn(Future.successful(Spawned))

      post(spawnSeqCompRequest) ~> route ~> check {
        verify(securityDirective).sPost(EswUserRolePolicy())
        responseAs[SpawnResponse] should ===(Spawned)
      }
    }

    "be able to send failure response when agent is not found | ESW-361" in {
      val spawnSeqCompRequest = SpawnSequenceComponent(agentPrefix, sequenceCompName, None)

      when(securityDirective.sPost(EswUserRolePolicy())).thenReturn(dummyDirective)
      when(agentService.spawnSequenceComponent(agentPrefix, sequenceCompName, None))
        .thenReturn(Future.successful(failedResponse))

      post(spawnSeqCompRequest) ~> route ~> check {
        verify(securityDirective).sPost(EswUserRolePolicy())
        responseAs[SpawnResponse] should ===(failedResponse)
      }
    }

  }

  "KillComponent" must {
    val componentId          = ComponentId(Prefix(randomSubsystem, randomString(10)), Service)
    val connection           = AkkaConnection(componentId)
    val stopComponentRequest = KillComponent(connection)

    "be able to stop component of the given componentId | ESW-361" in {
      when(securityDirective.sPost(EswUserRolePolicy())).thenReturn(dummyDirective)
      when(agentService.killComponent(connection)).thenReturn(Future.successful(Killed))

      post(stopComponentRequest) ~> route ~> check {
        verify(securityDirective).sPost(EswUserRolePolicy())
        responseAs[KillResponse] should ===(Killed)
      }
    }

    "be able to send failure response when agent is not found | ESW-361" in {
      when(securityDirective.sPost(EswUserRolePolicy())).thenReturn(dummyDirective)
      when(agentService.killComponent(connection)).thenReturn(Future.successful(failedResponse))

      post(stopComponentRequest) ~> route ~> check {
        verify(securityDirective).sPost(EswUserRolePolicy())
        responseAs[KillResponse] should ===(failedResponse)
      }
    }
  }
}

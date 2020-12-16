package esw.agent.service.app.handlers

import java.nio.file.Path

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.directives.BasicDirectives
import akka.http.scaladsl.testkit.ScalatestRouteTest
import csw.aas.http.SecurityDirectives
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Service
import csw.prefix.models.Prefix
import esw.agent.service.api.AgentServiceApi
import esw.agent.service.api.codecs.AgentServiceCodecs
import esw.agent.service.api.models._
import esw.agent.service.api.protocol.AgentServiceRequest
import esw.agent.service.api.protocol.AgentServiceRequest._
import esw.agent.service.app.auth.EswUserRolePolicy
import esw.testcommons.BaseTestSuite
import msocket.api.ContentType
import msocket.http.post.{ClientHttpCodecs, PostRouteFactory}
import msocket.jvm.metrics.LabelExtractor
import msocket.security.models.AccessToken

import scala.concurrent.Future

class AgentPostHandlerTest extends BaseTestSuite with ScalatestRouteTest with AgentServiceCodecs with ClientHttpCodecs {

  def clientContentType: ContentType = ContentType.Json

  private val agentService: AgentServiceApi = mock[AgentServiceApi]
  private val securityDirective             = mock[SecurityDirectives]

  import LabelExtractor.Implicits.default
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

  "SpawnAlarmServer" must {

    val sentinelConfPath        = Path.of(randomString(5))
    val redisPort               = Some(9090)
    val spawnAlarmServerRequest = SpawnAlarmServer(agentPrefix, sentinelConfPath, redisPort, None)

    "be able to start a alarm service | ESW-368" in {

      when(securityDirective.sPost(EswUserRolePolicy())).thenReturn(dummyDirective)
      when(agentService.spawnAlarmServer(agentPrefix, sentinelConfPath, redisPort, None))
        .thenReturn(Future.successful(Spawned))

      post(spawnAlarmServerRequest) ~> route ~> check {
        verify(securityDirective).sPost(EswUserRolePolicy())
        responseAs[SpawnResponse] should ===(Spawned)
      }
    }

    "be able to send failure response when agent is not found | ESW-368" in {

      when(securityDirective.sPost(EswUserRolePolicy())).thenReturn(dummyDirective)
      when(agentService.spawnAlarmServer(agentPrefix, sentinelConfPath, redisPort, None))
        .thenReturn(Future.successful(failedResponse))

      post(spawnAlarmServerRequest) ~> route ~> check {
        verify(securityDirective).sPost(EswUserRolePolicy())
        responseAs[SpawnResponse] should ===(failedResponse)
      }
    }
  }

  "SpawnEventServer" must {

    val sentinelConfPath        = Path.of(randomString(5))
    val redisPort               = Some(9090)
    val spawnEventServerRequest = SpawnEventServer(agentPrefix, sentinelConfPath, redisPort, None)

    "be able to start a event  service | ESW-368" in {

      when(securityDirective.sPost(EswUserRolePolicy())).thenReturn(dummyDirective)
      when(agentService.spawnEventServer(agentPrefix, sentinelConfPath, redisPort, None))
        .thenReturn(Future.successful(Spawned))

      post(spawnEventServerRequest) ~> route ~> check {
        verify(securityDirective).sPost(EswUserRolePolicy())
        responseAs[SpawnResponse] should ===(Spawned)
      }
    }

    "be able to send failure response when agent is not found | ESW-368" in {

      when(securityDirective.sPost(EswUserRolePolicy())).thenReturn(dummyDirective)
      when(agentService.spawnEventServer(agentPrefix, sentinelConfPath, redisPort, None))
        .thenReturn(Future.successful(failedResponse))

      post(spawnEventServerRequest) ~> route ~> check {
        verify(securityDirective).sPost(EswUserRolePolicy())
        responseAs[SpawnResponse] should ===(failedResponse)
      }
    }
  }

  "SpawnPostgres" must {

    val pgDataConfPath             = Path.of(randomString(5))
    val postgresPort               = Some(9090)
    val dbUnixSocketDirs           = "/tmp"
    val spawnPostgresServerRequest = SpawnPostgres(agentPrefix, pgDataConfPath, postgresPort, dbUnixSocketDirs, None)

    "be able to start a event  service | ESW-368" in {

      when(securityDirective.sPost(EswUserRolePolicy())).thenReturn(dummyDirective)
      when(agentService.spawnPostgres(agentPrefix, pgDataConfPath, postgresPort, dbUnixSocketDirs, None))
        .thenReturn(Future.successful(Spawned))

      post(spawnPostgresServerRequest) ~> route ~> check {
        verify(securityDirective).sPost(EswUserRolePolicy())
        responseAs[SpawnResponse] should ===(Spawned)
      }
    }

    "be able to send failure response when agent is not found | ESW-368" in {

      when(securityDirective.sPost(EswUserRolePolicy())).thenReturn(dummyDirective)
      when(agentService.spawnPostgres(agentPrefix, pgDataConfPath, postgresPort, dbUnixSocketDirs, None))
        .thenReturn(Future.successful(failedResponse))

      post(spawnPostgresServerRequest) ~> route ~> check {
        verify(securityDirective).sPost(EswUserRolePolicy())
        responseAs[SpawnResponse] should ===(failedResponse)
      }
    }
  }

  "KillComponent" must {
    val componentId          = ComponentId(Prefix(randomSubsystem, randomString(10)), Service)
    val stopComponentRequest = KillComponent(componentId)

    "be able to stop component of the given componentId | ESW-361" in {
      when(securityDirective.sPost(EswUserRolePolicy())).thenReturn(dummyDirective)
      when(agentService.killComponent(componentId)).thenReturn(Future.successful(Killed))

      post(stopComponentRequest) ~> route ~> check {
        verify(securityDirective).sPost(EswUserRolePolicy())
        responseAs[KillResponse] should ===(Killed)
      }
    }

    "be able to send failure response when agent is not found | ESW-361" in {
      when(securityDirective.sPost(EswUserRolePolicy())).thenReturn(dummyDirective)
      when(agentService.killComponent(componentId)).thenReturn(Future.successful(failedResponse))

      post(stopComponentRequest) ~> route ~> check {
        verify(securityDirective).sPost(EswUserRolePolicy())
        responseAs[KillResponse] should ===(failedResponse)
      }
    }
  }
}

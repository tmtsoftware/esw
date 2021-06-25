package esw.gateway.server

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.ClassicActorSystemOps
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import csw.command.client.auth.CommandRoles
import csw.gateway.mock.AuthMocks
import csw.location.api.models.{ComponentId, ComponentType}
import csw.logging.models.Level
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.{Prefix, Subsystem}
import esw.gateway.api.AdminApi
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.GatewayRequest
import esw.gateway.api.protocol.GatewayRequest._
import esw.gateway.server.handlers.GatewayPostHandler
import esw.testcommons.BaseTestSuite
import msocket.api.ContentType
import msocket.http.post.{ClientHttpCodecs, PostRouteFactory}
import msocket.jvm.metrics.LabelExtractor
import org.scalatest.prop.TableFor3
import org.scalatest.prop.Tables.Table

import scala.concurrent.Future

class GatewayAuthRequestTest extends BaseTestSuite with ScalatestRouteTest with GatewayCodecs with ClientHttpCodecs {
  override def clientContentType: ContentType = ContentType.Json

  implicit val typedSystem: ActorSystem[_] = system.toTyped
  private val cswCtxMocks                  = new CswTestMocks()
  import cswCtxMocks._

  private val mocks = new AuthMocks()
  import mocks._
  private val commandRoles = CommandRoles.empty

  private val postHandlerImpl =
    new GatewayPostHandler(alarmApi, resolver, eventApi, loggingApi, adminApi, securityDirectives, commandRoles)

  import LabelExtractor.Implicits.default
  private val route = new PostRouteFactory("post-endpoint", postHandlerImpl).make()
  private def post[E: ToEntityMarshaller](entity: E): HttpRequest =
    Post("/post-endpoint", entity).addHeader(Authorization(OAuth2BearerToken(token)))

  override def afterEach(): Unit = {
    reset(adminApi, access)
    super.afterEach()
  }

  "GatewayPostHandler" must {
    val componentType = randomFrom(List(ComponentType.Assembly, ComponentType.HCD, ComponentType.Container))
    val subsystem     = randomSubsystem
    val componentId   = ComponentId(Prefix(subsystem, randomString(10)), componentType)

    val data: TableFor3[String, GatewayRequest, AdminApi => Future[Done]] = Table(
      ("Name", "command", "api"),
      ("GoOffline", GoOffline(componentId), _.goOffline(componentId)),
      ("GoOnline", GoOnline(componentId), _.goOnline(componentId)),
      ("Shutdown", Shutdown(componentId), _.shutdown(componentId)),
      ("Restart", Restart(componentId), _.restart(componentId))
    )

    data.foreach {
      case (name, command, api) =>
        s"authorize $name if it has subsystem eng role | ESW-378" in {
          when(api(adminApi)).thenReturn(Future.successful(Done))
          val roles = Set(s"$subsystem-eng")
          stubRolesInAccessToken(roles)

          post(command: GatewayRequest) ~> route ~> check {
            api(verify(adminApi))
            status shouldEqual StatusCodes.OK
            responseAs[Done] shouldEqual Done
          }
        }
    }

    data.foreach {
      case (name, command, api) =>
        s"authorize $name if it has esw user role | ESW-378" in {
          when(api(adminApi)).thenReturn(Future.successful(Done))
          val roles = Set("ESW-user")
          stubRolesInAccessToken(roles)

          post(command: GatewayRequest) ~> route ~> check {
            api(verify(adminApi))
            status shouldEqual StatusCodes.OK
            responseAs[Done] shouldEqual Done
          }
        }
    }

    data.foreach {
      case (name, command, api) =>
        s"not authorize $name if it doesn't have the given component's subsystem eng role or ESW user Role | ESW-378" in {
          val subsystemsWithoutCurrentSubsystem = Subsystem.values.filterNot(_ == subsystem).toList
          val subsystemsWithoutESW              = Subsystem.values.filterNot(_ == ESW).toList
          val userRoles                         = subsystemsWithoutESW.map(x => s"$x-user")
          val engRoles                          = subsystemsWithoutCurrentSubsystem.map(x => s"$x-eng")
          stubRolesInAccessToken((userRoles ::: engRoles).toSet)

          post(command: GatewayRequest) ~> route ~> check {
            api(verify(adminApi, never))
            rejection shouldEqual AuthorizationFailedRejection
          }
        }
    }

    "authorize setLoglevel if it has esw user role | ESW-378" in {
      val level = randomFrom(Level.values.toList)
      when(adminApi.setLogLevel(componentId, level)).thenReturn(Future.successful(Done))
      val roles = Set("ESW-user")
      stubRolesInAccessToken(roles)

      post(SetLogLevel(componentId, level): GatewayRequest) ~> route ~> check {
        verify(adminApi).setLogLevel(componentId, level)
        status shouldEqual StatusCodes.OK
        responseAs[Done] shouldEqual Done
      }
    }

    "authorize setLoglevel if it has subsystem user role | ESW-378" in {
      val level = randomFrom(Level.values.toList)
      when(adminApi.setLogLevel(componentId, level)).thenReturn(Future.successful(Done))
      val roles = Set(s"$subsystem-user")
      stubRolesInAccessToken(roles)

      post(SetLogLevel(componentId, level): GatewayRequest) ~> route ~> check {
        verify(adminApi).setLogLevel(componentId, level)
        status shouldEqual StatusCodes.OK
        responseAs[Done] shouldEqual Done
      }
    }

    s"not authorize setLoglevel if it doesn't have the given component's subsystem or ESW user role | ESW-378" in {
      val level             = randomFrom(Level.values.toList)
      val subsystemForRoles = Subsystem.values.filterNot(x => x == subsystem || x == ESW).toList
      val roles             = subsystemForRoles.map(x => s"$x-user").toSet
      stubRolesInAccessToken(roles)

      post(SetLogLevel(componentId, level): GatewayRequest) ~> route ~> check {
        verify(adminApi, never).setLogLevel(componentId, level)
        rejection shouldEqual AuthorizationFailedRejection
      }
    }
  }
}

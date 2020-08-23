package esw.gateway.server

import java.nio.file.Path

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.server.Route
import csw.aas.http.SecurityDirectives
import csw.command.client.auth.CommandRoles
import csw.config.client.commons.ConfigUtils
import csw.config.client.scaladsl.ConfigClientFactory
import csw.location.client.ActorSystemFactory
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.{GatewayRequest, GatewayStreamRequest}
import esw.gateway.api.{AdminApi, AlarmApi, EventApi, LoggingApi}
import esw.gateway.impl.{AdminImpl, AlarmImpl, EventImpl, LoggerCache, LoggingImpl}
import esw.gateway.server.handlers.{GatewayPostHandler, GatewayWebsocketHandler}
import esw.gateway.server.utils.Resolver
import esw.http.core.wiring.{HttpService, ServerWiring}
import msocket.api.StreamRequestHandler
import msocket.impl.RouteFactory
import msocket.impl.post.{HttpPostHandler, PostRouteFactory}
import msocket.impl.ws.WebsocketRouteFactory

class GatewayWiring(_port: Option[Int], local: Boolean, commandRoleConfigPath: Path, metricsEnabled: Boolean = false)
    extends GatewayCodecs {
  private[server] lazy val actorSystem: ActorSystem[SpawnProtocol.Command] =
    ActorSystemFactory.remote(SpawnProtocol(), "gateway-system")

  lazy val wiring = new ServerWiring(_port, actorSystem = actorSystem)
  import wiring._
  import cswWiring._
  import cswWiring.actorRuntime.{ec, typedSystem}

  private[esw] val resolver = new Resolver(locationService)(actorSystem)

  lazy val alarmApi: AlarmApi     = new AlarmImpl(alarmService)
  lazy val eventApi: EventApi     = new EventImpl(eventService, eventSubscriberUtil)
  lazy val loggingApi: LoggingApi = new LoggingImpl(new LoggerCache)
  lazy val adminApi: AdminApi     = new AdminImpl(locationService)

  private lazy val configClient            = ConfigClientFactory.clientApi(actorSystem, locationService)
  private lazy val configUtils             = new ConfigUtils(configClient)
  private lazy val commandRolesConfig      = configUtils.getConfig(commandRoleConfigPath, local)
  private lazy val commandRoles            = commandRolesConfig.map(CommandRoles.from)
  private[esw] lazy val securityDirectives = SecurityDirectives(actorSystem.settings.config, cswWiring.locationService)

  lazy val postHandler: HttpPostHandler[GatewayRequest] =
    new GatewayPostHandler(alarmApi, resolver, eventApi, loggingApi, adminApi, securityDirectives, commandRoles)

  lazy val websocketHandlerFactory: StreamRequestHandler[GatewayStreamRequest] = new GatewayWebsocketHandler(resolver, eventApi)

  lazy val routes: Route = RouteFactory.combine(metricsEnabled)(
    new PostRouteFactory[GatewayRequest]("post-endpoint", postHandler),
    new WebsocketRouteFactory[GatewayStreamRequest]("websocket-endpoint", websocketHandlerFactory)
  )

  lazy val httpService = new HttpService(logger, locationService, routes, settings, actorRuntime)
}

object GatewayWiring {
  private[esw] def make(
      _port: Option[Int],
      local: Boolean,
      commandRoleConfigPath: Path,
      _actorSystem: ActorSystem[SpawnProtocol.Command],
      _securityDirectives: SecurityDirectives
  ): GatewayWiring =
    new GatewayWiring(_port, local, commandRoleConfigPath) {
      override lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = _actorSystem
      override private[esw] lazy val securityDirectives                 = _securityDirectives
    }

  private[esw] def make(
      _port: Option[Int],
      local: Boolean,
      commandRoleConfigPath: Path,
      _actorSystem: ActorSystem[SpawnProtocol.Command]
  ): GatewayWiring =
    new GatewayWiring(_port, local, commandRoleConfigPath) {
      override lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = _actorSystem
    }
}

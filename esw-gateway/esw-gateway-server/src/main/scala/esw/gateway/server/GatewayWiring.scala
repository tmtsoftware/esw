package esw.gateway.server

import java.nio.file.Path

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.server.Route
import csw.aas.http.SecurityDirectives
import csw.alarm.api.scaladsl.AlarmService
import csw.alarm.client.AlarmServiceFactory
import csw.command.client.auth.CommandRoles
import csw.config.client.commons.ConfigUtils
import csw.config.client.scaladsl.ConfigClientFactory
import csw.event.api.scaladsl.EventService
import csw.event.client.EventServiceFactory
import csw.event.client.internal.commons.EventSubscriberUtil
import csw.event.client.models.EventStores.RedisStore
import csw.location.api.models.ComponentType
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.{GatewayRequest, GatewayStreamRequest}
import esw.gateway.api.{AdminApi, AlarmApi, EventApi, LoggingApi}
import esw.gateway.impl._
import esw.gateway.server.handlers.{GatewayPostHandler, GatewayWebsocketHandler}
import esw.gateway.server.utils.Resolver
import esw.http.core.wiring.{ActorRuntime, HttpService, Settings}
import io.lettuce.core.RedisClient
import msocket.http.RouteFactory
import msocket.http.post.{HttpPostHandler, PostRouteFactory}
import msocket.http.ws.WebsocketRouteFactory
import msocket.jvm.stream.StreamRequestHandler

import scala.concurrent.Future

class GatewayWiring(_port: Option[Int], local: Boolean, commandRoleConfigPath: Path, metricsEnabled: Boolean = false)
    extends GatewayCodecs
    with GatewayRequestLabels
    with GatewayStreamRequestLabels {

  private[server] lazy val actorSystem: ActorSystem[SpawnProtocol.Command] =
    ActorSystemFactory.remote(SpawnProtocol(), "gateway-system")

  lazy val actorRuntime = new ActorRuntime(actorSystem)
  import actorRuntime.{ec, typedSystem}

  private lazy val config = actorSystem.settings.config
  lazy val settings       = new Settings(_port, None, config, ComponentType.Service)

  private lazy val loggerFactory = new LoggerFactory(settings.httpConnection.prefix)
  lazy val logger: Logger        = loggerFactory.getLogger

  private lazy val redisClient: RedisClient = {
    val client = RedisClient.create()
    shutdownRedisOnTermination(client)
    client
  }

  private lazy val locationService: LocationService         = HttpLocationServiceFactory.makeLocalClient
  private lazy val eventSubscriberUtil: EventSubscriberUtil = new EventSubscriberUtil()
  private lazy val eventServiceFactory: EventServiceFactory = new EventServiceFactory(RedisStore(redisClient))
  private lazy val eventService: EventService               = eventServiceFactory.make(locationService)

  private lazy val alarmServiceFactory: AlarmServiceFactory = new AlarmServiceFactory(redisClient)
  private lazy val alarmService: AlarmService               = alarmServiceFactory.makeClientApi(locationService)

  lazy val alarmApi: AlarmApi     = new AlarmImpl(alarmService)
  lazy val eventApi: EventApi     = new EventImpl(eventService, eventSubscriberUtil)
  lazy val loggingApi: LoggingApi = new LoggingImpl(new LoggerCache)
  lazy val adminApi: AdminApi     = new AdminImpl(locationService)

  private lazy val configClient            = ConfigClientFactory.clientApi(actorSystem, locationService)
  private lazy val configUtils             = new ConfigUtils(configClient)
  private lazy val commandRolesConfig      = configUtils.getConfig(commandRoleConfigPath, local)
  private[esw] lazy val commandRoles       = commandRolesConfig.map(CommandRoles.from)
  private[esw] lazy val securityDirectives = SecurityDirectives(actorSystem.settings.config, locationService)

  private[esw] val resolver = new Resolver(locationService)(actorSystem)

  lazy val postHandler: HttpPostHandler[GatewayRequest] =
    new GatewayPostHandler(alarmApi, resolver, eventApi, loggingApi, adminApi, securityDirectives, commandRoles)

  lazy val websocketHandlerFactory: StreamRequestHandler[GatewayStreamRequest] = new GatewayWebsocketHandler(resolver, eventApi)

  lazy val httpService = new HttpService(logger, locationService, routes, settings, actorRuntime)
  lazy val routes: Route = RouteFactory.combine(metricsEnabled)(
    new PostRouteFactory[GatewayRequest]("post-endpoint", postHandler),
    new WebsocketRouteFactory[GatewayStreamRequest]("websocket-endpoint", websocketHandlerFactory)
  )

  private def shutdownRedisOnTermination(client: RedisClient): Unit =
    actorRuntime.coordinatedShutdown.addTask(
      CoordinatedShutdown.PhaseBeforeServiceUnbind,
      "redis-client-shutdown"
    )(() => Future { client.shutdown(); Done })
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

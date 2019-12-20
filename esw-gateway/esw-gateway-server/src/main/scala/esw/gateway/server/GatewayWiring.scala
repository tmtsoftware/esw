package esw.gateway.server

import akka.http.scaladsl.server.Route
import csw.admin.api.AdminService
import csw.admin.impl.AdminServiceImpl
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.{PostRequest, WebsocketRequest}
import esw.gateway.api.{AlarmApi, EventApi, LoggingApi}
import esw.gateway.impl._
import esw.gateway.server.handlers.{PostHandlerImpl, WebsocketHandlerImpl}
import esw.gateway.server.utils.Resolver
import esw.http.core.wiring.{HttpService, ServerWiring}
import msocket.api.Encoding
import msocket.impl.RouteFactory
import msocket.impl.post.PostRouteFactory
import msocket.impl.ws.WebsocketRouteFactory

class GatewayWiring(_port: Option[Int]) extends GatewayCodecs {
  lazy val wiring = new ServerWiring(_port)
  import wiring._
  import cswWiring._
  import cswWiring.actorRuntime.{ec, typedSystem}

  private val resolver = new Resolver(locationService)(actorSystem)

  lazy val alarmApi: AlarmApi     = new AlarmImpl(alarmService)
  lazy val eventApi: EventApi     = new EventImpl(eventService, eventSubscriberUtil)
  lazy val loggingApi: LoggingApi = new LoggingImpl(new LoggerCache)
  lazy val adminApi: AdminService = new AdminServiceImpl(locationService)

  lazy val postHandler: PostHandlerImpl = new PostHandlerImpl(alarmApi, resolver, eventApi, loggingApi, adminApi)
  def websocketHandlerFactory(encoding: Encoding[_]): WebsocketHandlerImpl =
    new WebsocketHandlerImpl(resolver, eventApi, encoding)

  lazy val routes: Route = RouteFactory.combine(
    new PostRouteFactory[PostRequest]("post-endpoint", postHandler),
    new WebsocketRouteFactory[WebsocketRequest]("websocket-endpoint", websocketHandlerFactory)
  )

  lazy val httpService = new HttpService(logger, locationService, routes, settings, actorRuntime)
}

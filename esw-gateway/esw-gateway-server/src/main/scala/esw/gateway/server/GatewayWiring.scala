package esw.gateway.server

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Source
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.{PostRequest, WebsocketRequest}
import esw.gateway.api.{AlarmApi, EventApi, LoggingApi}
import esw.gateway.impl._
import esw.gateway.server.handlers.{PostHandlerImpl, WebsocketHandlerImpl}
import esw.gateway.server.utils.CommandServiceResolver
import esw.http.core.wiring.{HttpService, ServerWiring}
import msocket.api.MessageHandler
import msocket.impl.post.PostRouteFactory
import msocket.impl.ws.WebsocketRouteFactory
import msocket.impl.{Encoding, RouteFactory}

class GatewayWiring(_port: Option[Int]) extends GatewayCodecs {
  lazy val wiring = new ServerWiring(_port)
  import wiring._
  import cswWiring._
  import cswWiring.actorRuntime.{ec, typedSystem}

  private val commandServiceResolver = new CommandServiceResolver(locationService)(actorSystem)

  lazy val alarmApi: AlarmApi     = new AlarmImpl(alarmService)
  lazy val eventApi: EventApi     = new EventImpl(eventService, eventSubscriberUtil)
  lazy val loggingApi: LoggingApi = new LoggingImpl(new LoggerCache)

  lazy val postHandler: MessageHandler[PostRequest, Route] =
    new PostHandlerImpl(alarmApi, commandServiceResolver, eventApi, loggingApi)
  def websocketHandlerFactory(encoding: Encoding[_]): MessageHandler[WebsocketRequest, Source[Message, NotUsed]] =
    new WebsocketHandlerImpl(commandServiceResolver, eventApi, encoding)

  lazy val routes: Route = RouteFactory.combine(
    new PostRouteFactory("post-endpoint", postHandler),
    new WebsocketRouteFactory("websocket-endpoint", websocketHandlerFactory)
  )

  lazy val httpService = new HttpService(logger, locationService, routes, settings, actorRuntime)
}

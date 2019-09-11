package esw.gateway.server

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.StandardRoute
import akka.stream.scaladsl.Source
import akka.util.Timeout
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.{PostRequest, WebsocketRequest}
import esw.gateway.api.{AlarmApi, CommandApi, EventApi}
import esw.gateway.impl.{AlarmImpl, CommandImpl, EventImpl}
import esw.gateway.server.handlers.{PostHandlerImpl, WebsocketHandlerImpl}
import esw.http.core.wiring.{HttpService, ServerWiring}
import msocket.api.RequestHandler

import scala.concurrent.duration.DurationLong

class GatewayWiring(_port: Option[Int] = None) extends GatewayCodecs {
  lazy val wiring = new ServerWiring(_port)
  import wiring._
  import cswCtx.actorRuntime.{ec, mat}
  import cswCtx.{actorRuntime, _}
  implicit val timeout: Timeout = 10.seconds

  lazy val alarmApi: AlarmApi     = new AlarmImpl(alarmService)
  lazy val eventApi: EventApi     = new EventImpl(eventService, eventSubscriberUtil)
  lazy val commandApi: CommandApi = new CommandImpl(componentFactory.commandService)

  lazy val postHandler: RequestHandler[PostRequest, StandardRoute] =
    new PostHandlerImpl(alarmApi, commandApi, eventApi)
  lazy val websocketHandler: RequestHandler[WebsocketRequest, Source[Message, NotUsed]] =
    new WebsocketHandlerImpl(commandApi, eventApi)

  lazy val routes      = new Routes(postHandler, websocketHandler, logger)
  lazy val httpService = new HttpService(logger, locationService, routes.route, settings, actorRuntime)
}

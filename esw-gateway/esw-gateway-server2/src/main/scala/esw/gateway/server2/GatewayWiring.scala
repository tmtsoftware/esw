package esw.gateway.server2

import akka.http.scaladsl.server.StandardRoute
import akka.util.Timeout
import esw.gateway.api.codecs.RestlessCodecs
import esw.gateway.api.messages.{PostRequest, WebsocketRequest}
import esw.gateway.api.{AlarmServiceApi, CommandServiceApi, EventServiceApi}
import esw.gateway.impl.{AlarmServiceImpl, CommandServiceImpl, EventServiceImpl}
import esw.http.core.wiring.{HttpService, ServerWiring}
import mscoket.impl.RoutesFactory
import msocket.api.{PostHandler, WebsocketHandler}

import scala.concurrent.duration.DurationLong

class GatewayWiring(_port: Option[Int] = None) extends RestlessCodecs {
  lazy val wiring = new ServerWiring(_port)
  import wiring._
  import cswCtx.actorRuntime.{ec, mat}
  import cswCtx.{actorRuntime, _}
  implicit val timeout: Timeout = 10.seconds

  lazy val alarmServiceApi: AlarmServiceApi     = new AlarmServiceImpl(alarmService)
  lazy val eventServiceApi: EventServiceApi     = new EventServiceImpl(eventService, eventSubscriberUtil)
  lazy val commandServiceApi: CommandServiceApi = new CommandServiceImpl(componentFactory.commandService)

  lazy val httpHandler: PostHandler[PostRequest, StandardRoute] =
    new PostHandlerImpl(alarmServiceApi, commandServiceApi, eventServiceApi)
  lazy val websocketHandler: WebsocketHandler[WebsocketRequest] =
    new WebsocketHandlerImpl(commandServiceApi, eventServiceApi)

  lazy val routesFactory: RoutesFactory[PostRequest, WebsocketRequest] = new RoutesFactory(httpHandler, websocketHandler)
  lazy val httpService                                                 = new HttpService(logger, locationService, routesFactory.route, settings, actorRuntime)
}

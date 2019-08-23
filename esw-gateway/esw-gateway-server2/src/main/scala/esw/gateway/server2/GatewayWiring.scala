package esw.gateway.server2

import akka.util.Timeout
import esw.gateway.api.{AlarmServiceApi, CommandServiceApi, EventServiceApi}
import esw.gateway.impl.{AlarmServiceImpl, CommandServiceImpl, EventServiceImpl}
import esw.http.core.wiring.{HttpService, ServerWiring}

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

class GatewayWiring(_port: Option[Int]) {

  private[esw] def start(): HttpService = {
    lazy val wiring = new ServerWiring(_port)
    import wiring._
    import cswCtx.{actorRuntime, _}
    import cswCtx.actorRuntime.{ec, mat}
    implicit val timeout: Timeout = 10.seconds

    lazy val alarmServiceApi: AlarmServiceApi     = new AlarmServiceImpl(alarmService)
    lazy val eventServiceApi: EventServiceApi     = new EventServiceImpl(eventService, eventSubscriberUtil)
    lazy val commandServiceApi: CommandServiceApi = new CommandServiceImpl(componentFactory.commandService)
    lazy val routes: GatewayRoutes                = new GatewayRoutes(alarmServiceApi, commandServiceApi, eventServiceApi)

    val httpService = new HttpService(logger, locationService, routes.route, settings, actorRuntime)
    Await.result(httpService.registeredLazyBinding, 15.seconds)
    httpService
  }

}

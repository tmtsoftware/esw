package esw.gateway.server2

import esw.gateway.api.GatewayApi
import esw.gateway.impl.{AlarmGatewayImpl, CommandGatewayImpl, EventGatewayImpl}
import esw.http.core.utils.CswContext
import esw.http.core.wiring.{HttpService, ServerWiring}

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

class GatewayWiring(_port: Option[Int]) {

  private[esw] def start(): HttpService = {
    lazy val wiring = new ServerWiring(_port)
    import wiring._
    import wiring.cswCtx.{locationService, logger}

    lazy val gatewayImpl: GatewayApi = new CommandGatewayImpl with AlarmGatewayImpl with EventGatewayImpl {
      override val cswContext: CswContext = cswCtx
    }

    lazy val routes: GatewayRoutes = new GatewayRoutes(gatewayImpl)

    val httpService = new HttpService(logger, locationService, routes.route, settings, actorRuntime)
    Await.result(httpService.registeredLazyBinding, 15.seconds)
    httpService
  }

}

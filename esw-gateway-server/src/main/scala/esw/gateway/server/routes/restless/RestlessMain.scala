package esw.gateway.server.routes.restless

import esw.gateway.server.routes.restless.impl.GatewayImpl
import esw.http.core.wiring.{HttpService, ServerWiring}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object RestlessMain {

  def main(args: Array[String]): Unit = {
    start(Some(8040))
  }

  private[esw] def start(port: Option[Int]): HttpService = {
    lazy val wiring = new ServerWiring(port)
    import wiring._
    import wiring.cswCtx.{locationService, logger}
    lazy val gatewayApi     = new GatewayImpl(cswCtx)
    lazy val routes: Routes = new Routes(gatewayApi)

    val httpService = new HttpService(logger, locationService, routes.route, settings, actorRuntime)

    Await.result(httpService.registeredLazyBinding, 15.seconds)
    httpService
  }
}

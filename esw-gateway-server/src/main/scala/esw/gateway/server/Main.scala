package esw.gateway.server

import esw.gateway.server.routes.Routes
import esw.http.core.cli.{ArgsParser, Options}
import esw.http.core.wiring.{HttpService, ServerWiring}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object Main {

  def main(args: Array[String]): Unit = start(args, startLogging = true)

  def start(args: Array[String], startLogging: Boolean): Option[HttpService] = {
    new ArgsParser("http-server").parse(args.toList).map {
      case Options(port) =>
        val wiring = new ServerWiring(port)
        import wiring._
        import wiring.cswCtx._
        if (startLogging) actorRuntime.startLogging(BuildInfo.name, BuildInfo.version)

        lazy val routes      = new Routes(cswCtx)
        lazy val httpService = new HttpService(logger, locationService, routes.route, settings, actorRuntime)

        Await.result(httpService.registeredLazyBinding, 15.seconds)
        httpService
    }
  }
}

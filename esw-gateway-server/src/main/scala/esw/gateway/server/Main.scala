package esw.gateway.server

import esw.gateway.server.routes.Routes
import esw.template.http.server.cli.{ArgsParser, Options}
import esw.template.http.server.csw.utils.CswContext
import esw.template.http.server.wiring.HttpService

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object Main {

  def main(args: Array[String]): Unit = start(args, startLogging = true)

  def start(args: Array[String], startLogging: Boolean): Option[HttpService] = {
    new ArgsParser("http-server").parse(args).map {
      case Options(port) =>
        val cswContext = new CswContext(port)
        import cswContext._
        if (startLogging) actorRuntime.startLogging(BuildInfo.name, BuildInfo.version)

        lazy val routes      = new Routes(cswContext)
        lazy val httpService = new HttpService(logger, locationService, routes.route, settings, actorRuntime)

        Await.result(httpService.registeredLazyBinding, 15.seconds)
        httpService
    }
  }
}

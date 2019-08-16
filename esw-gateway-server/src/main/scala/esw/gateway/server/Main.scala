package esw.gateway.server

import caseapp._
import esw.gateway.server.ServerCommand.StartCommand
import esw.gateway.server.routes.Routes
import esw.http.core.wiring.{HttpService, ServerWiring}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object Main extends CommandApp[ServerCommand] {
  override def appName: String    = getClass.getSimpleName.dropRight(1) // remove $ from class name
  override def appVersion: String = BuildInfo.version
  override def progName: String   = BuildInfo.name

  def run(command: ServerCommand, args: RemainingArgs): Unit =
    command match {
      case StartCommand(port) => start(port, startLogging = true)
    }

  private[esw] def start(port: Option[Int], startLogging: Boolean): HttpService = {
    val wiring = new ServerWiring(port)
    import wiring._
    import wiring.cswCtx.{locationService, logger}
    if (startLogging) actorRuntime.startLogging(BuildInfo.name, BuildInfo.version)

    lazy val routes      = new Routes(cswCtx)
    lazy val httpService = new HttpService(logger, locationService, routes.route, settings, actorRuntime)

    Await.result(httpService.registeredLazyBinding, 15.seconds)
    httpService
  }
}

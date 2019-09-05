package esw.gateway.server

import caseapp.{CommandApp, RemainingArgs}
import esw.gateway.server.ServerCommand.StartCommand

object GatewayMain extends CommandApp[ServerCommand] {
  override def appName: String    = getClass.getSimpleName.dropRight(1) // remove $ from class name
  override def appVersion: String = BuildInfo.version
  override def progName: String   = BuildInfo.name

  override def run(command: ServerCommand, args: RemainingArgs): Unit =
    command match {
      case StartCommand(port) => start(port, startLogging = true)
    }

  def start(port: Option[Int], startLogging: Boolean): Unit = {
    val gatewayWiring = new GatewayWiring()

    import gatewayWiring._
    import gatewayWiring.wiring.actorRuntime
    if (startLogging) actorRuntime.startLogging(BuildInfo.name, BuildInfo.version)

    httpService.registeredLazyBinding
  }

}

package esw.gateway.server

import java.nio.file.Path

import caseapp.RemainingArgs
import esw.gateway.server.ServerCommand.StartCommand
import esw.http.core.commons.EswCommandApp

// $COVERAGE-OFF$
object GatewayMain extends EswCommandApp[ServerCommand] {
  override def appName: String    = getClass.getSimpleName.dropRight(1) // remove $ from class name
  override def appVersion: String = BuildInfo.version
  override def progName: String   = BuildInfo.name

  override def run(command: ServerCommand, args: RemainingArgs): Unit =
    command match {
      case StartCommand(port, local, commandRoleConfigPath, metricsEnabled) =>
        start(port, local, commandRoleConfigPath, metricsEnabled, startLogging = true)
    }

  def start(
      port: Option[Int],
      local: Boolean,
      commandRoleConfigPath: Path,
      metricsEnabled: Boolean,
      startLogging: Boolean
  ): Unit =
    start(new GatewayWiring(port, local, commandRoleConfigPath, metricsEnabled), startLogging)

  private[esw] def start(gatewayWiring: GatewayWiring, startLogging: Boolean): Unit = {
    import gatewayWiring._
    if (startLogging) actorRuntime.startLogging(BuildInfo.name, BuildInfo.version)

    httpService.startAndRegisterServer()
  }

}
// $COVERAGE-ON$

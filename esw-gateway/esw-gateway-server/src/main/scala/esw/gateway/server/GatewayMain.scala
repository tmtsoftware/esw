package esw.gateway.server

import caseapp.{Command, RemainingArgs}
import caseapp.core.app.CommandsEntryPoint
import caseapp.core.help.Help
import caseapp.core.parser.Parser
import esw.commons.cli.EswCommand
import esw.gateway.server.ServerCommand.StartOptions

import java.nio.file.Path

// $COVERAGE-OFF$

/**
 * Main app to start gateway server
 */
object GatewayMain extends CommandsEntryPoint {
  def appName: String           = getClass.getSimpleName.dropRight(1) // remove $ from class name
  def appVersion: String        = BuildInfo.version
  override def progName: String = BuildInfo.name

  val StartCommand: Runner[StartOptions] = Runner[StartOptions]()

  override def commands: Seq[Command[?]] = List(StartCommand)

  class Runner[T <: ServerCommand: Parser: Help] extends EswCommand[T] {
    override def run(command: T, args: RemainingArgs): Unit = {
      command match {
        case StartOptions(port, local, commandRoleConfigPath, metricsEnabled) =>
          start(port, local, commandRoleConfigPath, metricsEnabled, startLogging = true)
      }
    }

    def start(
        port: Option[Int],
        local: Boolean,
        commandRoleConfigPath: Path,
        metricsEnabled: Boolean,
        startLogging: Boolean
    ): GatewayWiring =
      start(new GatewayWiring(port, local, commandRoleConfigPath, metricsEnabled), startLogging)

    private[esw] def start(gatewayWiring: GatewayWiring, startLogging: Boolean): GatewayWiring = {
      import gatewayWiring._
      if (startLogging) actorRuntime.startLogging(BuildInfo.name, BuildInfo.version)

      httpService.startAndRegisterServer()
      gatewayWiring
    }

  }

}
// $COVERAGE-ON$

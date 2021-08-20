package esw.gateway.server

import java.nio.file.Path

import caseapp._

/**
 * ServerCommand - a set of command line param written using case app for the gateway server.
 */
sealed trait ServerCommand

object ServerCommand {
  @CommandName("start")
  final case class StartCommand(
      @ExtraName("p")
      @HelpMessage(
        "HTTP server will be bound to this port. " +
          "If a value is not provided, it will be picked up from configuration"
      )
      port: Option[Int],
      @ExtraName("l")
      @HelpMessage("use command role mapping file from local file system else fetch it from config service")
      local: Boolean = false,
      @ExtraName("c")
      @HelpMessage(
        "specifies command role mapping file path which gets fetched from config service or local file system based on --local option"
      )
      commandRoleConfigPath: Path,
      @ExtraName("m")
      @HelpMessage("Enable gateway metrics")
      metrics: Boolean = false
  ) extends ServerCommand
}

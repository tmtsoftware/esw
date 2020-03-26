package esw.gateway.server

import caseapp._

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
      @ExtraName("m")
      @HelpMessage("Enable gateway metrics")
      metrics: Boolean = false
  ) extends ServerCommand
}

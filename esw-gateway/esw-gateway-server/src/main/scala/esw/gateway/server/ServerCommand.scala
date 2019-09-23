package esw.gateway.server

import caseapp._

sealed trait ServerCommand

object ServerCommand {
  @CommandName("start")
  final case class StartCommand(
      @HelpMessage(
        "HTTP server will be bound to this port. " +
          "If a value is not provided, it will be picked up from configuration"
      )
      port: Option[Int]
  ) extends ServerCommand
}

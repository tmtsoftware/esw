package esw.agent.service.app

import caseapp.{CommandName, ExtraName, HelpMessage}

/**
 * AgentServiceAppCommand - a set of command line param written using case app for the Agent Service App
 */
sealed trait AgentServiceAppCommand

object AgentServiceAppCommand {

  final case class StartOptions(
      @ExtraName("p")
      @HelpMessage(
        "optional argument: port on which HTTP server will be bound. " +
          "If a value is not provided, it will be randomly picked."
      )
      port: Option[Int] = None
  ) extends AgentServiceAppCommand
}

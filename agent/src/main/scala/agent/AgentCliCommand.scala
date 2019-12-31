package agent

import caseapp.{CommandName, HelpMessage}

sealed trait AgentCliCommand

object AgentCliCommand {
  @CommandName("start")
  final case class StartCommand(
      //todo: @dolly
      @HelpMessage(
        "TODO"
      )
      machineName: String
  ) extends AgentCliCommand
}

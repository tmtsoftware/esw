package agent

import caseapp.{CommandName, HelpMessage, ExtraName => Short}

sealed trait AgentCliCommand

object AgentCliCommand {
  @CommandName("start")
  final case class StartCommand(
      @HelpMessage("name for machine component, ex: ocs1, tcs_primary etc")
      @Short("n")
      machineName: String
  ) extends AgentCliCommand
}

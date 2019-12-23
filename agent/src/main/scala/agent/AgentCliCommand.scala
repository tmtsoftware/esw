package agent

import caseapp.{CommandName, HelpMessage}

sealed trait AgentCliCommand

object AgentCliCommand {
  @CommandName("start")
  final case class StartCommand(
      @HelpMessage(
        "Optional port at which Akka cluster will run. " +
          "If a value is not provided, it will be picked up from configuration"
      )
      clusterPort: Option[Int]
  ) extends AgentCliCommand
}

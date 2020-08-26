package esw.agent.service.app

import caseapp.CommandName

sealed trait AgentServiceAppCommand

object AgentServiceAppCommand {

  @CommandName("start")
  final case class StartCommand() extends AgentServiceAppCommand
}

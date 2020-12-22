package esw.services

import caseapp.{CommandName, ExtraName, HelpMessage}

sealed trait Command

object Command {

  @CommandName("start")
  final case class Start(
      @ExtraName("a")
      @HelpMessage(
        "start agent"
      )
      agentPrefix: Option[String]
  ) extends Command
}

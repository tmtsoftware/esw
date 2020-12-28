package esw.services

import caseapp.{CommandName, ExtraName, HelpMessage}

import java.nio.file.Path

sealed trait Command

object Command {

  @CommandName("start")
  final case class Start(
      @ExtraName("a")
      @HelpMessage(
        "start agent"
      )
      agentPrefix: Option[String],
      @ExtraName("g")
      @HelpMessage(
        "start gateway with specified command role mapping file path from local"
      )
      commandRoleConfigPath: Option[Path]
  ) extends Command
}

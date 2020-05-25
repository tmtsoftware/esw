package esw.sm.app

import java.nio.file.Path

import caseapp.{CommandName, ExtraName, HelpMessage}

sealed trait SequenceManagerAppCommand

object SequenceManagerAppCommand {
  @CommandName("start")
  final case class StartCommand(
      @ExtraName("p")
      @HelpMessage(
        "Config file path which has mapping of sequencers and resources needed for different observing modes"
      )
      observingModeResourcesConfigPath: Path
  ) extends SequenceManagerAppCommand
}

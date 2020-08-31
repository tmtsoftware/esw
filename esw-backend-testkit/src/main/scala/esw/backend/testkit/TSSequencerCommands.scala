package esw.backend.testkit

import caseapp.core.Error
import caseapp.core.argparser.SimpleArgParser
import caseapp.{CommandName, ExtraName, HelpMessage}
import csw.prefix.models.Subsystem

import scala.util.Try

sealed trait TSSequencerCommands
object TSSequencerCommands {
  implicit val subsystemParser: SimpleArgParser[Subsystem] =
    SimpleArgParser.from[Subsystem]("subsystem") { subsystemStr =>
      Try(Right(Subsystem.withNameInsensitive(subsystemStr)))
        .getOrElse(Left(Error.Other(s"Subsystem [$subsystemStr] is invalid")))
    }

  @CommandName("start")
  final case class Start(
      @HelpMessage("subsystem to start the sequencer")
      @ExtraName("s")
      subSystem: Subsystem,
      @HelpMessage("observing mode to start the sequencer")
      @ExtraName("m")
      observingMode: String
  ) extends TSSequencerCommands
}

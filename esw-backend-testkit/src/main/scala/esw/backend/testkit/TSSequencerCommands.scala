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
      @HelpMessage("subsystem of the sequencer")
      @ExtraName("s")
      subSystem: Subsystem,
      @HelpMessage("component name of the sequencer")
      @ExtraName("m")
      componentName: String
  ) extends TSSequencerCommands
}

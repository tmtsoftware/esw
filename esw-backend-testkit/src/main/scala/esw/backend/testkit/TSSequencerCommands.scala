package esw.backend.testkit

import caseapp.core.Error
import caseapp.core.argparser.SimpleArgParser
import caseapp.{CommandName, ExtraName, HelpMessage}
import csw.prefix.models.Subsystem
import esw.ocs.api.models.{ObsMode, Variation}

import scala.util.Try

sealed trait TSSequencerCommands
object TSSequencerCommands {
  implicit val subsystemParser: SimpleArgParser[Subsystem] =
    SimpleArgParser.from[Subsystem]("subsystem") { subsystemStr =>
      Try(Right(Subsystem.withNameInsensitive(subsystemStr)))
        .getOrElse(Left(Error.Other(s"Subsystem [$subsystemStr] is invalid")))
    }

  implicit val obsModeParser: SimpleArgParser[ObsMode] =
    SimpleArgParser.from[ObsMode]("obsMode") { obsModeName =>
      Right(ObsMode(obsModeName))
    }

  implicit val variationParser: SimpleArgParser[Variation] =
    SimpleArgParser.from[Variation]("variation") { variation =>
      Right(Variation(variation))
    }

  @CommandName("start")
  final case class Start(
      @HelpMessage("subsystem of the sequencer")
      @ExtraName("s")
      subSystem: Subsystem,
      @HelpMessage("obsmode of the sequencer")
      @ExtraName("m")
      obsMode: ObsMode,
      @HelpMessage("variation part of the sequencer")
      @ExtraName("v")
      variation: Option[Variation]
  ) extends TSSequencerCommands
}

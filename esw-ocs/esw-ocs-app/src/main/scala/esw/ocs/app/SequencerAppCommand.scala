package esw.ocs.app

import caseapp.core.Error
import caseapp.core.argparser.SimpleArgParser
import caseapp.{CommandName, HelpMessage, ExtraName => Short}
import csw.params.core.models.Subsystem

import scala.util.Try

sealed trait SequencerAppCommand {
  def subsystem: Subsystem
  def name: Option[String]
}

object SequencerAppCommand {

  implicit val subsystemParser: SimpleArgParser[Subsystem] =
    SimpleArgParser.from[Subsystem]("subsystem") { subsystemStr =>
      Try(Right(Subsystem.withNameInsensitive(subsystemStr)))
        .getOrElse(Left(Error.Other(s"Subsystem [$subsystemStr] is invalid")))
    }

  implicit val stringParser: SimpleArgParser[String] =
    SimpleArgParser.from[String](description = "string field") { str =>
      val invalidSymbol = "@"
      if (str.contains(invalidSymbol)) Left(Error.Other(s"[$str] is invalid"))
      else Right(str)
    }

  @CommandName("seqcomp")
  final case class SequenceComponent(
      @HelpMessage("subsystem of the sequence component, ex: tcs")
      @Short("s")
      subsystem: Subsystem,
      @HelpMessage("optional name for sequence component, ex: primary, backup etc")
      @Short("n")
      name: Option[String]
  ) extends SequencerAppCommand

  final case class Sequencer(
      @HelpMessage("subsystem of the sequence component, ex: tcs")
      @Short("s")
      subsystem: Subsystem,
      @HelpMessage("optional name for sequence component, ex: primary, backup etc")
      @Short("n")
      name: Option[String],
      @HelpMessage("optional package ID of script, ex: tcs, iris etc. Default value: subsystem provided")
      @Short("i")
      id: Option[String],
      @HelpMessage("observing mode, ex: darknight")
      @Short("m")
      mode: String
  ) extends SequencerAppCommand

}

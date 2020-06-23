package esw.ocs.app

import caseapp.core.Error
import caseapp.core.argparser.SimpleArgParser
import caseapp.{CommandName, HelpMessage, ExtraName => Short}
import csw.prefix.models.Subsystem

import scala.util.Try

sealed trait SequencerAppCommand {
  def seqCompSubsystem: Subsystem
  def name: Option[String]
}

object SequencerAppCommand {

  implicit val subsystemParser: SimpleArgParser[Subsystem] =
    SimpleArgParser.from[Subsystem]("subsystem") { subsystemStr =>
      Try(Right(Subsystem.withNameInsensitive(subsystemStr)))
        .getOrElse(Left(Error.Other(s"Subsystem [$subsystemStr] is invalid")))
    }

  @CommandName("seqcomp")
  final case class SequenceComponent(
      @HelpMessage("subsystem of the sequence component, ex: tcs")
      @Short("s")
      seqCompSubsystem: Subsystem,
      @HelpMessage("optional name for sequence component, ex: primary, backup etc")
      @Short("n")
      name: Option[String]
  ) extends SequencerAppCommand

  final case class Sequencer(
      @HelpMessage("subsystem of the sequence component, ex: tcs")
      @Short("s")
      seqCompSubsystem: Subsystem,
      @HelpMessage("optional name for sequence component, ex: primary, backup etc")
      @Short("n")
      name: Option[String],
      @HelpMessage("optional subsystem of sequencer script, ex: tcs, iris etc. Default value: subsystem provided")
      @Short("i")
      seqSubsystem: Option[Subsystem],
      @HelpMessage("observing mode, ex: darknight")
      @Short("m")
      obsMode: String,
      @HelpMessage("simulation mode")
      @Short("simulation")
      simulation: Boolean = false
  ) extends SequencerAppCommand

}

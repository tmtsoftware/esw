package esw.ocs.app

import caseapp.core.Error
import caseapp.core.argparser.SimpleArgParser
import caseapp.{CommandName, HelpMessage}
import csw.params.core.models.Subsystem

import scala.util.control.NonFatal

sealed trait SequencerAppCommand

object SequencerAppCommand {

  implicit val subsystemParser: SimpleArgParser[Subsystem] = {
    SimpleArgParser.from[Subsystem]("subsystem") { subsystemStr =>
      try Right(Subsystem.withNameInsensitive(subsystemStr))
      catch {
        case NonFatal(_) => Left(Error.Other(s"Subsystem [$subsystemStr] is invalid"))
      }
    }
  }

  @CommandName("seqcomp")
  final case class SequenceComponent(
      @HelpMessage("subsystem of the sequence component, ex: tcs")
      subsystem: Subsystem,
      @HelpMessage("optional name for sequence component, ex: primary, backup etc")
      name: Option[String]
  ) extends SequencerAppCommand

  final case class Sequencer(
      @HelpMessage("sequencer ID, ex: iris")
      id: String,
      @HelpMessage("observing mode, ex: darknight")
      mode: String
  ) extends SequencerAppCommand

}

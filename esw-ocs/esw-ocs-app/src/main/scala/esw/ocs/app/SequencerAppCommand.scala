package esw.ocs.app

import caseapp.core.Error
import caseapp.core.argparser.{ArgParser, SimpleArgParser}
import caseapp.{CommandName, HelpMessage}
import csw.params.core.models.Prefix

import scala.util.control.NonFatal

sealed trait SequencerAppCommand

object SequencerAppCommand {

  implicit val prefixParser: ArgParser[Prefix] =
    SimpleArgParser.from[Prefix]("prefix") { prefixStr =>
      try Right(Prefix(prefixStr))
      catch {
        case NonFatal(_) => Left(Error.Other(s"Prefix: $prefixStr is invalid"))
      }
    }

  @CommandName("seqcomp")
  final case class SequenceComponent(
      @HelpMessage("prefix of the sequence component, ex: tcs.mobie.blue.filter")
      prefix: Prefix
  ) extends SequencerAppCommand

  final case class Sequencer(
      @HelpMessage("sequencer ID, ex: iris")
      id: String,
      @HelpMessage("observing mode, ex: darknight")
      mode: String
  ) extends SequencerAppCommand

}

package esw.ocs.app

import caseapp.{CommandName, HelpMessage}

sealed trait SequencerAppCommand

object SequencerAppCommand {

  @CommandName("seqcomp")
  final case class SequenceComponent(
      @HelpMessage("unique id of the sequence component(in Integer), ex: 1")
      id: Int
  ) extends SequencerAppCommand

  final case class Sequencer(
      @HelpMessage("sequencer ID, ex: iris")
      id: String,
      @HelpMessage("observing mode, ex: darknight")
      mode: String
  ) extends SequencerAppCommand

}

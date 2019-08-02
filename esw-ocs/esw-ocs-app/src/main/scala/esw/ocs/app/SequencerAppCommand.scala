package esw.ocs.app

import caseapp.{CommandName, HelpMessage}

sealed trait SequencerAppCommand

object SequencerAppCommand {

  @CommandName("seqcomp")
  final case class SequenceComponent(
      @HelpMessage("prefix of the sequence component, ex: tcs.mobie.blue.filter")
      prefix: String
  ) extends SequencerAppCommand

  final case class Sequencer(
      @HelpMessage("sequencer ID, ex: iris")
      id: String,
      @HelpMessage("observing mode, ex: darknight")
      mode: String
  ) extends SequencerAppCommand

}

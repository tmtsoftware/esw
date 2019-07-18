package esw.ocs.framework.cli

import caseapp.{CommandName, HelpMessage}

sealed trait SequencerAppCommand

object SequencerAppCommand {

  @CommandName("seqcomp")
  final case class SequenceComponent(
      @HelpMessage("name of the sequence component")
      name: String
  ) extends SequencerAppCommand

  final case class Sequencer(
      @HelpMessage("sequencer ID, ex: iris")
      id: String,
      @HelpMessage("observing mode, ex: darknight")
      mode: String
  ) extends SequencerAppCommand

}

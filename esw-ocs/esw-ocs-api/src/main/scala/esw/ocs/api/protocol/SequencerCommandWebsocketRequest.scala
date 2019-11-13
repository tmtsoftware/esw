package esw.ocs.api.protocol

import csw.params.commands.Sequence

sealed trait SequencerCommandWebsocketRequest

private[ocs] object SequencerCommandWebsocketRequest {
  case object QueryFinal extends SequencerCommandWebsocketRequest
}

sealed trait SequencerCommandPostRequest
private[ocs] object SequencerCommandPostRequest {
  case class SubmitSequence(sequence: Sequence) extends SequencerCommandPostRequest
  case class LoadSequence(sequence: Sequence)   extends SequencerCommandPostRequest
  case object StartSequence                     extends SequencerCommandPostRequest
}

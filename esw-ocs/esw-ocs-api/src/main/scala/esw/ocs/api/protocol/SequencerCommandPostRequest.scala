package esw.ocs.api.protocol

import csw.params.commands.Sequence
import csw.time.core.models.UTCTime

sealed trait SequencerCommandPostRequest
private[ocs] object SequencerCommandPostRequest {
  case class SubmitSequence(sequence: Sequence)     extends SequencerCommandPostRequest
  final case class LoadSequence(sequence: Sequence) extends SequencerCommandPostRequest
  case object StartSequence                         extends SequencerCommandPostRequest

  case object GoOnline                                              extends SequencerCommandPostRequest
  case object GoOffline                                             extends SequencerCommandPostRequest
  final case class DiagnosticMode(startTime: UTCTime, hint: String) extends SequencerCommandPostRequest
  case object OperationsMode                                        extends SequencerCommandPostRequest
}

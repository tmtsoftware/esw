package esw.ocs.api.protocol

import csw.params.commands.{Sequence, SequenceCommand}
import csw.params.core.models.Id
import csw.time.core.models.UTCTime

sealed trait SequencerPostRequest
private[ocs] object SequencerPostRequest {
  // Admin Protocol
  case object GetSequence                                               extends SequencerPostRequest
  case object IsAvailable                                               extends SequencerPostRequest
  case object IsOnline                                                  extends SequencerPostRequest
  case object Pause                                                     extends SequencerPostRequest
  case object Resume                                                    extends SequencerPostRequest
  case object Reset                                                     extends SequencerPostRequest
  case object AbortSequence                                             extends SequencerPostRequest
  case object Stop                                                      extends SequencerPostRequest
  final case class Add(commands: List[SequenceCommand])                 extends SequencerPostRequest
  final case class Prepend(commands: List[SequenceCommand])             extends SequencerPostRequest
  final case class Replace(id: Id, commands: List[SequenceCommand])     extends SequencerPostRequest
  final case class InsertAfter(id: Id, commands: List[SequenceCommand]) extends SequencerPostRequest
  final case class Delete(id: Id)                                       extends SequencerPostRequest
  final case class AddBreakpoint(id: Id)                                extends SequencerPostRequest
  final case class RemoveBreakpoint(id: Id)                             extends SequencerPostRequest

  // Command Protocol
  case class Submit(sequence: Sequence)                             extends SequencerPostRequest
  final case class LoadSequence(sequence: Sequence)                 extends SequencerPostRequest
  case object StartSequence                                         extends SequencerPostRequest
  case class Query(runId: Id)                                       extends SequencerPostRequest
  case object GoOnline                                              extends SequencerPostRequest
  case object GoOffline                                             extends SequencerPostRequest
  final case class DiagnosticMode(startTime: UTCTime, hint: String) extends SequencerPostRequest
  case object OperationsMode                                        extends SequencerPostRequest

}

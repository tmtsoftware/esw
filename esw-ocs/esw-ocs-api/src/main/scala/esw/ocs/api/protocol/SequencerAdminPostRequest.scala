package esw.ocs.api.protocol

import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import csw.time.core.models.UTCTime

sealed trait SequencerAdminPostRequest
private[ocs] object SequencerAdminPostRequest {
  case object GetSequence                                               extends SequencerAdminPostRequest
  case object IsAvailable                                               extends SequencerAdminPostRequest
  case object IsOnline                                                  extends SequencerAdminPostRequest
  case object Pause                                                     extends SequencerAdminPostRequest
  case object Resume                                                    extends SequencerAdminPostRequest
  case object Reset                                                     extends SequencerAdminPostRequest
  case object AbortSequence                                             extends SequencerAdminPostRequest
  case object Stop                                                      extends SequencerAdminPostRequest
  case object GoOnline                                                  extends SequencerAdminPostRequest
  case object GoOffline                                                 extends SequencerAdminPostRequest
  case object OperationsMode                                            extends SequencerAdminPostRequest
  final case class Add(commands: List[SequenceCommand])                 extends SequencerAdminPostRequest
  final case class Prepend(commands: List[SequenceCommand])             extends SequencerAdminPostRequest
  final case class Replace(id: Id, commands: List[SequenceCommand])     extends SequencerAdminPostRequest
  final case class InsertAfter(id: Id, commands: List[SequenceCommand]) extends SequencerAdminPostRequest
  final case class Delete(id: Id)                                       extends SequencerAdminPostRequest
  final case class AddBreakpoint(id: Id)                                extends SequencerAdminPostRequest
  final case class RemoveBreakpoint(id: Id)                             extends SequencerAdminPostRequest
  final case class DiagnosticMode(startTime: UTCTime, hint: String)     extends SequencerAdminPostRequest
}

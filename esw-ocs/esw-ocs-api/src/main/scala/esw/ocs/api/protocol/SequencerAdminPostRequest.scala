package esw.ocs.api.protocol

import csw.params.commands.{Sequence, SequenceCommand}
import csw.params.core.models.Id

sealed trait SequencerAdminPostRequest
private[ocs] object SequencerAdminPostRequest {
  case object GetSequence                                         extends SequencerAdminPostRequest
  case object IsAvailable                                         extends SequencerAdminPostRequest
  case object IsOnline                                            extends SequencerAdminPostRequest
  case object Pause                                               extends SequencerAdminPostRequest
  case object Resume                                              extends SequencerAdminPostRequest
  case object Reset                                               extends SequencerAdminPostRequest
  case object AbortSequence                                       extends SequencerAdminPostRequest
  case object GoOnline                                            extends SequencerAdminPostRequest
  case object GoOffline                                           extends SequencerAdminPostRequest
  case object StartSequence                                       extends SequencerAdminPostRequest
  case class LoadSequence(sequence: Sequence)                     extends SequencerAdminPostRequest
  case class LoadAndStartSequence(sequence: Sequence)             extends SequencerAdminPostRequest
  case class Add(commands: List[SequenceCommand])                 extends SequencerAdminPostRequest
  case class Prepend(commands: List[SequenceCommand])             extends SequencerAdminPostRequest
  case class Replace(id: Id, commands: List[SequenceCommand])     extends SequencerAdminPostRequest
  case class InsertAfter(id: Id, commands: List[SequenceCommand]) extends SequencerAdminPostRequest
  case class Delete(id: Id)                                       extends SequencerAdminPostRequest
  case class AddBreakpoint(id: Id)                                extends SequencerAdminPostRequest
  case class RemoveBreakpoint(id: Id)                             extends SequencerAdminPostRequest
}

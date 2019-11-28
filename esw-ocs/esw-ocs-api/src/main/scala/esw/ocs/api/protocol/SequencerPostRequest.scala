package esw.ocs.api.protocol

import csw.params.commands.{Sequence, SequenceCommand}
import csw.params.core.models.Id
import csw.time.core.models.UTCTime

sealed trait SequencerPostRequest

object SequencerPostRequest {

  case class LoadSequence(sequence: Sequence) extends SequencerPostRequest
  case object StartSequence                   extends SequencerPostRequest

  case object GetSequence                                         extends SequencerPostRequest
  case class Add(commands: List[SequenceCommand])                 extends SequencerPostRequest
  case class Prepend(commands: List[SequenceCommand])             extends SequencerPostRequest
  case class Replace(id: Id, commands: List[SequenceCommand])     extends SequencerPostRequest
  case class InsertAfter(id: Id, commands: List[SequenceCommand]) extends SequencerPostRequest
  case class Delete(id: Id)                                       extends SequencerPostRequest
  case class AddBreakpoint(id: Id)                                extends SequencerPostRequest
  case class RemoveBreakpoint(id: Id)                             extends SequencerPostRequest
  case object Reset                                               extends SequencerPostRequest
  case object Pause                                               extends SequencerPostRequest
  case object Resume                                              extends SequencerPostRequest

  case object IsAvailable   extends SequencerPostRequest
  case object IsOnline      extends SequencerPostRequest
  case object GoOnline      extends SequencerPostRequest
  case object GoOffline     extends SequencerPostRequest
  case object AbortSequence extends SequencerPostRequest
  case object Stop          extends SequencerPostRequest

  case class DiagnosticMode(startTime: UTCTime, hint: String) extends SequencerPostRequest
  case object OperationsMode                                  extends SequencerPostRequest

  // Sequencer Command Protocol
  case class Submit(sequence: Sequence) extends SequencerPostRequest
  case class Query(runId: Id)           extends SequencerPostRequest

}

package esw.ocs.api.protocol

import csw.params.commands.{Sequence, SequenceCommand}
import csw.params.core.models.Id
import csw.time.core.models.UTCTime

/**
 * These models are being used as Http requests(POST) for the sequencer
 */
sealed trait SequencerRequest

object SequencerRequest {

  case class LoadSequence(sequence: Sequence) extends SequencerRequest
  case object StartSequence                   extends SequencerRequest

  case object GetSequence                                         extends SequencerRequest
  case class Add(commands: List[SequenceCommand])                 extends SequencerRequest
  case class Prepend(commands: List[SequenceCommand])             extends SequencerRequest
  case class Replace(id: Id, commands: List[SequenceCommand])     extends SequencerRequest
  case class InsertAfter(id: Id, commands: List[SequenceCommand]) extends SequencerRequest
  case class Delete(id: Id)                                       extends SequencerRequest
  case class AddBreakpoint(id: Id)                                extends SequencerRequest
  case class RemoveBreakpoint(id: Id)                             extends SequencerRequest
  case object Reset                                               extends SequencerRequest
  case object Pause                                               extends SequencerRequest
  case object Resume                                              extends SequencerRequest
  case object GetSequenceComponent                                extends SequencerRequest
  case object GetSequencerState                                   extends SequencerRequest

  case object IsAvailable   extends SequencerRequest
  case object IsOnline      extends SequencerRequest
  case object GoOnline      extends SequencerRequest
  case object GoOffline     extends SequencerRequest
  case object AbortSequence extends SequencerRequest
  case object Stop          extends SequencerRequest

  case class DiagnosticMode(startTime: UTCTime, hint: String) extends SequencerRequest
  case object OperationsMode                                  extends SequencerRequest

  // Sequencer Command Protocol
  case class Submit(sequence: Sequence) extends SequencerRequest
  case class Query(runId: Id)           extends SequencerRequest

}

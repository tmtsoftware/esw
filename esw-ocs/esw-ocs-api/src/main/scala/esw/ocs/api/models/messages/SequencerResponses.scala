package esw.ocs.api.models.messages

import csw.params.core.models.Id
import esw.ocs.api.models.{Step, StepList}
import esw.ocs.api.serializer.OcsFrameworkAkkaSerializable

sealed trait SimpleResponse

sealed trait UpdateSequencerStateResponse
sealed trait LoadSequenceResponse
sealed trait PullNextResponse
sealed trait MaybeNextResponse
sealed trait GetSequenceResponse
sealed trait GetPreviousSequenceResponse
sealed trait UpdateFailureResponse

case object Ok extends SimpleResponse with LoadSequenceResponse

case class GetSequenceResult(stepList: StepList)                 extends GetSequenceResponse
case class GetPreviousSequenceResult(stepList: Option[StepList]) extends GetPreviousSequenceResponse
case class PullNextResult(step: Step)                            extends PullNextResponse
case class MaybeNextResult(step: Option[Step])                   extends MaybeNextResponse

sealed case class Unhandled(state: String, messageType: String)
    extends SimpleResponse
    with UpdateSequencerStateResponse
    with LoadSequenceResponse
    with PullNextResponse
    with MaybeNextResponse
    with UpdateFailureResponse
    with GetSequenceResponse
    with GetPreviousSequenceResponse {
  val description = s"Sequencer can not accept '$messageType' message in '$state' state"
}

sealed trait SequencerError

// load-and-start sequence error
case object DuplicateIdsFound extends LoadSequenceResponse with SequencerError {
  val description = "Duplicate command Ids found in given sequence"
}

sealed trait EditorError extends OcsFrameworkAkkaSerializable with SequencerError

object EditorError {
  sealed trait AddBreakpointError extends EditorError
  sealed trait InsertError        extends EditorError
  sealed trait ReplaceError       extends EditorError
  sealed trait DeleteError        extends EditorError

  case object CannotOperateOnAnInFlightOrFinishedStep extends AddBreakpointError with DeleteError
  case object CannotInsertOrReplaceAfterAFinishedStep extends InsertError with ReplaceError
  case class IdDoesNotExist(id: Id)                   extends ReplaceError with InsertError with DeleteError with AddBreakpointError
}

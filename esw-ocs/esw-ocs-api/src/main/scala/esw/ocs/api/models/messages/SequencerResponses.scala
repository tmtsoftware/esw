package esw.ocs.api.models.messages

import csw.params.commands.CommandResponse.{Error, SubmitResponse}
import csw.params.core.models.Id
import esw.ocs.api.models.{Step, StepList}
import esw.ocs.api.serializer.OcsFrameworkAkkaSerializable

sealed trait SimpleResponse  // fixme: think about better name
sealed trait ComplexResponse // fixme: think about better name
sealed trait PauseResponse
sealed trait RemoveBreakpointResponse

sealed trait LoadSequenceResponse
sealed trait PullNextResponse
sealed trait MaybeNextResponse
sealed trait GetSequenceResponse
sealed trait GetPreviousSequenceResponse
sealed trait SequenceResponse {
  def toSubmitResponse(sequenceId: Id): SubmitResponse = this match {
    case SequenceResult(submitResponse) => submitResponse
    case DuplicateIdsFound              => Error(sequenceId, DuplicateIdsFound.description)
    case unhandled: Unhandled           => Error(sequenceId, unhandled.description)
  }
}

case object Ok
    extends SimpleResponse
    with ComplexResponse
    with PauseResponse
    with RemoveBreakpointResponse
    with LoadSequenceResponse

case class GetSequenceResult(stepList: StepList)                 extends GetSequenceResponse
case class GetPreviousSequenceResult(stepList: Option[StepList]) extends GetPreviousSequenceResponse
case class PullNextResult(step: Step)                            extends PullNextResponse
case class MaybeNextResult(step: Option[Step])                   extends MaybeNextResponse
case class SequenceResult(submitResponse: SubmitResponse)        extends SequenceResponse

sealed case class Unhandled(state: String, messageType: String)
    extends SimpleResponse
    with ComplexResponse
    with PauseResponse
    with RemoveBreakpointResponse
    with LoadSequenceResponse
    with SequenceResponse
    with PullNextResponse
    with MaybeNextResponse
    with GetSequenceResponse
    with GetPreviousSequenceResponse {
  val description = s"Sequencer can not accept '$messageType' message in '$state' state"
}

// load-and-start sequence error
case object DuplicateIdsFound extends LoadSequenceResponse with SequenceResponse {
  val description = "Duplicate command Ids found in given sequence"
}

sealed trait EditorError extends ComplexResponse with OcsFrameworkAkkaSerializable

object EditorError {
  case object CannotOperateOnAnInFlightOrFinishedStep extends EditorError with PauseResponse
  case class IdDoesNotExist(id: Id)                   extends EditorError with RemoveBreakpointResponse
}

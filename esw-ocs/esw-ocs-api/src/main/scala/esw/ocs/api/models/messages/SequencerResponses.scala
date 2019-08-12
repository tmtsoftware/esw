package esw.ocs.api.models.messages

import csw.params.core.models.Id
import esw.ocs.api.models.{Step, StepList}
import esw.ocs.api.serializer.OcsFrameworkAkkaSerializable

sealed trait SimpleResponse // fixme: think about better name
sealed trait ComplexResponse // fixme: think about better name
sealed trait PauseResponse
sealed trait RemoveBreakpointResponse

sealed trait LoadSequenceResponse
sealed trait PullNextResponse
sealed trait MaybeNextResponse
sealed trait GetSequenceResponse
sealed trait GetPreviousSequenceResponse

case object Ok extends SimpleResponse with ComplexResponse with PauseResponse with RemoveBreakpointResponse with LoadSequenceResponse

case class GetSequenceResult(stepList: StepList)                 extends GetSequenceResponse
case class GetPreviousSequenceResult(stepList: Option[StepList]) extends GetPreviousSequenceResponse
case class PullNextResult(step: Step)                            extends PullNextResponse
case class MaybeNextResult(step: Option[Step])                   extends MaybeNextResponse

sealed case class Unhandled(state: String, messageType: String)
    extends SimpleResponse
    with ComplexResponse
    with PauseResponse
    with RemoveBreakpointResponse
    with LoadSequenceResponse
    with PullNextResponse
    with MaybeNextResponse
    with GetSequenceResponse
    with GetPreviousSequenceResponse {
  val description = s"Sequencer can not accept '$messageType' message in '$state' state"
}

// load-and-start sequence error
case object DuplicateIdsFound extends LoadSequenceResponse {
  val description = "Duplicate command Ids found in given sequence"
}

sealed trait EditorError extends ComplexResponse with OcsFrameworkAkkaSerializable

object EditorError {
  case object CannotOperateOnAnInFlightOrFinishedStep extends EditorError with PauseResponse
  case class IdDoesNotExist(id: Id)                   extends EditorError with RemoveBreakpointResponse
}

package esw.ocs.api.models.messages

import csw.params.commands.CommandResponse.{Error, SubmitResponse}
import csw.params.core.models.Id
import esw.ocs.api.models.{Step, StepList}
import esw.ocs.api.serializer.OcsFrameworkAkkaSerializable

sealed trait Response                    extends OcsFrameworkAkkaSerializable
sealed trait SimpleResponse              extends Response // fixme: think about better name
sealed trait ComplexResponse             extends Response // fixme: think about better name
sealed trait PauseResponse               extends Response
sealed trait RemoveBreakpointResponse    extends Response
sealed trait LoadSequenceResponse        extends Response
sealed trait PullNextResponse            extends Response
sealed trait MaybeNextResponse           extends Response
sealed trait GetSequenceResponse         extends Response
sealed trait GetPreviousSequenceResponse extends Response

sealed trait SequenceResponse extends Response {
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

case class Unhandled(state: String, messageType: String)
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

case object DuplicateIdsFound extends LoadSequenceResponse with SequenceResponse {
  val description = "Duplicate command Ids found in given sequence"
}

sealed trait EditorError extends ComplexResponse

object EditorError {
  case object CannotOperateOnAnInFlightOrFinishedStep extends EditorError with PauseResponse
  case class IdDoesNotExist(id: Id)                   extends EditorError with RemoveBreakpointResponse
}

package esw.ocs.api.models.messages

import csw.params.commands.CommandResponse.{Error, SubmitResponse}
import csw.params.core.models.Id
import esw.ocs.api.models.{Step, StepList}
import esw.ocs.api.serializer.OcsFrameworkAkkaSerializable

sealed trait SimpleResponse              extends OcsFrameworkAkkaSerializable // fixme: think about better name
sealed trait ComplexResponse             extends OcsFrameworkAkkaSerializable // fixme: think about better name
sealed trait PauseResponse               extends OcsFrameworkAkkaSerializable
sealed trait RemoveBreakpointResponse    extends OcsFrameworkAkkaSerializable
sealed trait LoadSequenceResponse        extends OcsFrameworkAkkaSerializable
sealed trait PullNextResponse            extends OcsFrameworkAkkaSerializable
sealed trait MaybeNextResponse           extends OcsFrameworkAkkaSerializable
sealed trait GetSequenceResponse         extends OcsFrameworkAkkaSerializable
sealed trait GetPreviousSequenceResponse extends OcsFrameworkAkkaSerializable

sealed trait SequenceResponse extends OcsFrameworkAkkaSerializable {
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

sealed trait EditorError extends ComplexResponse with OcsFrameworkAkkaSerializable

object EditorError {
  case object CannotOperateOnAnInFlightOrFinishedStep extends EditorError with PauseResponse
  case class IdDoesNotExist(id: Id)                   extends EditorError with RemoveBreakpointResponse
}

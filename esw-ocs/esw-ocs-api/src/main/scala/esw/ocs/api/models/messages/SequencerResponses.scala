package esw.ocs.api.models.messages

import csw.params.commands.CommandResponse.{Error, SubmitResponse}
import csw.params.core.models.Id
import esw.ocs.api.models.{Step, StepList}
import esw.ocs.api.serializer.OcsFrameworkAkkaSerializable

sealed trait EswSequencerResponse     extends OcsFrameworkAkkaSerializable with Product with Serializable
sealed trait OkOrUnhandledResponse    extends EswSequencerResponse // fixme: think about better name
sealed trait GenericResponse          extends EswSequencerResponse // fixme: think about better name
sealed trait PauseResponse            extends EswSequencerResponse
sealed trait RemoveBreakpointResponse extends EswSequencerResponse
sealed trait LoadSequenceResponse     extends EswSequencerResponse
sealed trait PullNextResponse         extends EswSequencerResponse
sealed trait MaybeNextResponse        extends EswSequencerResponse
sealed trait StepListResponse         extends EswSequencerResponse
sealed trait GoOnlineResponse         extends EswSequencerResponse

sealed trait SequenceResponse extends EswSequencerResponse {
  def toSubmitResponse(sequenceId: Id): SubmitResponse = this match {
    case SequenceResult(submitResponse) => submitResponse
    case DuplicateIdsFound              => Error(sequenceId, DuplicateIdsFound.description)
    case unhandled: Unhandled           => Error(sequenceId, unhandled.description)
  }
}

case object Ok
    extends OkOrUnhandledResponse
    with GenericResponse
    with PauseResponse
    with RemoveBreakpointResponse
    with LoadSequenceResponse
    with GoOnlineResponse

case class StepListResult(stepList: Option[StepList])     extends StepListResponse
case class PullNextResult(step: Step)                     extends PullNextResponse
case class MaybeNextResult(step: Option[Step])            extends MaybeNextResponse
case class SequenceResult(submitResponse: SubmitResponse) extends SequenceResponse

case class Unhandled(state: String, messageType: String)
    extends OkOrUnhandledResponse
    with GenericResponse
    with PauseResponse
    with RemoveBreakpointResponse
    with LoadSequenceResponse
    with GoOnlineResponse
    with SequenceResponse
    with PullNextResponse
    with MaybeNextResponse
    with StepListResponse {
  val description = s"Sequencer can not accept '$messageType' message in '$state' state"
}

case object DuplicateIdsFound extends LoadSequenceResponse with SequenceResponse {
  val description = "Duplicate command Ids found in given sequence"
}

case object GoOnlineFailed extends GoOnlineResponse {
  val description = s"Sequencer could not go online because online handlers failed to execute successfully"
}

sealed trait EditorError extends GenericResponse

object EditorError {
  case object CannotOperateOnAnInFlightOrFinishedStep extends EditorError with PauseResponse
  case class IdDoesNotExist(id: Id)                   extends EditorError with RemoveBreakpointResponse
}

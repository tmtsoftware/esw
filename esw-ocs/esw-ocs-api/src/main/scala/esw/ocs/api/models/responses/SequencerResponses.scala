package esw.ocs.api.models.responses

import csw.params.commands.CommandResponse.{Error, SubmitResponse}
import csw.params.core.models.Id
import esw.ocs.api.codecs.OcsAkkaSerializable
import esw.ocs.api.models.Step

sealed trait EswSequencerResponse     extends OcsAkkaSerializable
sealed trait OkOrUnhandledResponse    extends EswSequencerResponse // fixme: think about better name
sealed trait GenericResponse          extends EswSequencerResponse // fixme: think about better name
sealed trait PauseResponse            extends EswSequencerResponse
sealed trait RemoveBreakpointResponse extends EswSequencerResponse
sealed trait LoadSequenceResponse     extends EswSequencerResponse
sealed trait PullNextResponse         extends EswSequencerResponse
sealed trait GoOnlineResponse         extends EswSequencerResponse

sealed trait SequenceResponse extends EswSequencerResponse {
  def toSubmitResponse(sequenceId: Id): SubmitResponse = this match {
    case SequenceResult(submitResponse) => submitResponse
    case DuplicateIdsFound              => Error(sequenceId, DuplicateIdsFound.msg)
    case unhandled: Unhandled           => Error(sequenceId, unhandled.msg)
  }
}

case object Ok
    extends OkOrUnhandledResponse
    with GenericResponse
    with PauseResponse
    with RemoveBreakpointResponse
    with LoadSequenceResponse
    with GoOnlineResponse

case class PullNextResult(step: Step)                     extends PullNextResponse
case class SequenceResult(submitResponse: SubmitResponse) extends SequenceResponse

case class Unhandled private[ocs] (state: String, messageType: String, msg: String)
    extends OkOrUnhandledResponse
    with GenericResponse
    with PauseResponse
    with RemoveBreakpointResponse
    with LoadSequenceResponse
    with GoOnlineResponse
    with SequenceResponse
    with PullNextResponse

object Unhandled {
  def apply(state: String, messageType: String): Unhandled =
    new Unhandled(state, messageType, s"Sequencer can not accept '$messageType' message in '$state' state")

  private[ocs] def apply(state: String, messageType: String, description: String): Unhandled = {
    new Unhandled(state, messageType, s"Sequencer can not accept '$messageType' message in '$state' state")
  }
}

trait SingletonError {
  def msg: String
}

case object DuplicateIdsFound extends LoadSequenceResponse with SequenceResponse with SingletonError {
  val msg = "Duplicate command Ids found in given sequence"
}

case object GoOnlineHookFailed extends GoOnlineResponse with SingletonError {
  val msg = "Sequencer could not go online because online handlers failed to execute successfully"
}

sealed trait EditorError extends GenericResponse

object EditorError {
  case object CannotOperateOnAnInFlightOrFinishedStep extends EditorError with PauseResponse
  case class IdDoesNotExist(id: Id)                   extends EditorError with RemoveBreakpointResponse
}

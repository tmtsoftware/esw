package esw.ocs.api.protocol

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
sealed trait GoOfflineResponse        extends EswSequencerResponse
sealed trait DiagnosticModeResponse   extends EswSequencerResponse
sealed trait OperationsModeResponse   extends EswSequencerResponse

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
    with GoOfflineResponse
    with DiagnosticModeResponse
    with OperationsModeResponse

final case class PullNextResult(step: Step)                     extends PullNextResponse
final case class SequenceResult(submitResponse: SubmitResponse) extends SequenceResponse

final case class Unhandled private[ocs] (state: String, messageType: String, msg: String)
    extends OkOrUnhandledResponse
    with GenericResponse
    with PauseResponse
    with RemoveBreakpointResponse
    with LoadSequenceResponse
    with GoOnlineResponse
    with GoOfflineResponse
    with SequenceResponse
    with PullNextResponse

object Unhandled {
  def apply(state: String, messageType: String): Unhandled =
    new Unhandled(state, messageType, s"Sequencer can not accept '$messageType' message in '$state' state")

  private[ocs] def apply(state: String, messageType: String, description: String): Unhandled = {
    new Unhandled(state, messageType, description)
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

case object GoOfflineHookFailed extends GoOfflineResponse with SingletonError {
  val msg = "Sequencer could not go online because offline handlers failed to execute successfully"
}

case object DiagnosticHookFailed extends DiagnosticModeResponse with SingletonError {
  val msg = "Sequencer failed to execute diagnostic mode handlers."
}

case object OperationsHookFailed extends OperationsModeResponse with SingletonError {
  val msg = "Sequencer failed to execute operations mode handlers."
}

sealed trait EditorError extends GenericResponse

object EditorError {
  case object CannotOperateOnAnInFlightOrFinishedStep extends EditorError with PauseResponse
  final case class IdDoesNotExist(id: Id)             extends EditorError with RemoveBreakpointResponse
}

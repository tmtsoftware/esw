package esw.ocs.api.protocol

import csw.params.commands.CommandIssue.UnsupportedCommandInStateIssue
import csw.params.commands.CommandResponse.{Invalid, SubmitResponse}
import csw.params.core.models.Id
import esw.ocs.api.codecs.OcsAkkaSerializable
import esw.ocs.api.models.Step

sealed trait EswSequencerResponse     extends OcsAkkaSerializable
sealed trait OkOrUnhandledResponse    extends EswSequencerResponse // fixme: think about better name
sealed trait GenericResponse          extends EswSequencerResponse // fixme: think about better name
sealed trait PauseResponse            extends EswSequencerResponse
sealed trait RemoveBreakpointResponse extends EswSequencerResponse
sealed trait PullNextResponse         extends EswSequencerResponse
sealed trait GoOnlineResponse         extends EswSequencerResponse
sealed trait GoOfflineResponse        extends EswSequencerResponse
sealed trait DiagnosticModeResponse   extends EswSequencerResponse
sealed trait OperationsModeResponse   extends EswSequencerResponse

sealed trait SequencerSubmitResponse extends EswSequencerResponse {
  def toSubmitResponse(runId: Id = Id("IdNotAvailable")): SubmitResponse = this match {
    case SubmitResult(submitResponse) => submitResponse
    case unhandled: Unhandled         => Invalid(runId, UnsupportedCommandInStateIssue(unhandled.msg))
  }
}

case object Ok
    extends OkOrUnhandledResponse
    with GenericResponse
    with PauseResponse
    with RemoveBreakpointResponse
    with GoOnlineResponse
    with GoOfflineResponse
    with DiagnosticModeResponse
    with OperationsModeResponse

final case class PullNextResult(step: Step)                   extends PullNextResponse
final case class SubmitResult(submitResponse: SubmitResponse) extends SequencerSubmitResponse

final case class Unhandled private[ocs] (state: String, messageType: String, msg: String)
    extends OkOrUnhandledResponse
    with GenericResponse
    with PauseResponse
    with RemoveBreakpointResponse
    with GoOnlineResponse
    with GoOfflineResponse
    with SequencerSubmitResponse
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

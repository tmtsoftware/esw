package esw.ocs.api.protocol

import csw.params.commands.CommandIssue.UnsupportedCommandInStateIssue
import csw.params.commands.CommandResponse.{Error, Invalid, SubmitResponse}
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
  def toSubmitResponse(runId: Id = Id("IdNotAvailable")): SubmitResponse =
    this match {
      case SubmitResult(submitResponse) => submitResponse
      case unhandled: Unhandled         => Invalid(runId, UnsupportedCommandInStateIssue(unhandled.msg))
      case NewSequenceHookFailed(msg)   => Error(runId, msg)
    }
}

sealed trait Ok
    extends OkOrUnhandledResponse
    with GenericResponse
    with PauseResponse
    with RemoveBreakpointResponse
    with GoOnlineResponse
    with GoOfflineResponse
    with DiagnosticModeResponse
    with OperationsModeResponse

case object Ok extends Ok

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

case class GoOnlineHookFailed(msg: String) extends GoOnlineResponse

object GoOnlineHookFailed {
  def apply(): GoOnlineHookFailed =
    GoOnlineHookFailed("Sequencer could not go online because online handlers failed to execute successfully")
}

case class GoOfflineHookFailed(msg: String) extends GoOfflineResponse

object GoOfflineHookFailed {
  def apply(): GoOfflineHookFailed =
    GoOfflineHookFailed("Sequencer could not go offline because offline handlers failed to execute successfully")
}

case class DiagnosticHookFailed(msg: String) extends DiagnosticModeResponse

object DiagnosticHookFailed {
  def apply(): DiagnosticHookFailed =
    DiagnosticHookFailed("Sequencer failed to execute diagnostic mode handlers")
}

case class OperationsHookFailed(msg: String) extends OperationsModeResponse

object OperationsHookFailed {
  def apply(): OperationsHookFailed =
    OperationsHookFailed("Sequencer failed to execute operations mode handlers")
}

case class NewSequenceHookFailed(msg: String) extends SequencerSubmitResponse

object NewSequenceHookFailed {
  def apply(): NewSequenceHookFailed =
    new NewSequenceHookFailed("Sequence is not submitted because new sequence handler failed to execute successfully")
}

sealed trait EditorError extends GenericResponse

object EditorError {
  case object CannotOperateOnAnInFlightOrFinishedStep extends EditorError with PauseResponse
  final case class IdDoesNotExist(id: Id)             extends EditorError with RemoveBreakpointResponse
}

package esw.ocs.api.models.messages.error

import csw.params.core.models.Id
import esw.ocs.api.models.StepStatus
import esw.ocs.api.serializer.OcsFrameworkAkkaSerializable

sealed trait EditorError extends OcsFrameworkAkkaSerializable

final case class SequencerShutdownError(msg: String) extends EditorError
final case class SequencerAbortError(msg: String)    extends EditorError

sealed trait StepListError extends EditorError with Product with Serializable

object StepListError {

  final case class NotSupported(stepStatus: StepStatus)
      extends InsertError
      with ReplaceError
      with DeleteError
      with AddBreakpointError
      with PauseError

  case object NotAllowedOnFinishedSeq
      extends AddBreakpointError
      with PauseError
      with UpdateError
      with AddError
      with ResumeError
      with ResetError
      with ReplaceError
      with PrependError
      with DeleteError
      with InsertError
      with RemoveBreakpointError

  final case class IdDoesNotExist(id: Id)
      extends ReplaceError
      with InsertError
      with UpdateError
      with DeleteError
      with AddBreakpointError
      with RemoveBreakpointError

  sealed trait PauseError extends StepListError
  case object PauseFailed extends PauseError

  sealed trait UpdateError                                              extends StepListError
  final case class UpdateNotSupported(from: StepStatus, to: StepStatus) extends UpdateError

  sealed trait AddError extends StepListError
  case object AddFailed extends AddError

  sealed trait AddBreakpointError    extends StepListError
  sealed trait ResumeError           extends StepListError
  sealed trait PrependError          extends StepListError
  sealed trait ResetError            extends StepListError
  sealed trait InsertError           extends StepListError
  sealed trait ReplaceError          extends StepListError
  sealed trait DeleteError           extends StepListError
  sealed trait RemoveBreakpointError extends StepListError

}

package esw.ocs.api.models.messages

import esw.ocs.api.serializer.OcsFrameworkAkkaSerializable

import csw.params.core.models.Id
import esw.ocs.api.models.StepStatus

final case class RegistrationError(msg: String) extends OcsFrameworkAkkaSerializable

// Lifecycle Errors

sealed trait LifecycleError extends OcsFrameworkAkkaSerializable

final case class GoOnlineError(msg: String)  extends LifecycleError
final case class GoOfflineError(msg: String) extends LifecycleError
final case class ShutdownError(msg: String)  extends LifecycleError
final case class AbortError(msg: String)     extends LifecycleError

case object NotAllowedInOfflineState extends LifecycleError with EditorError

// Editor Errors
sealed trait EditorError extends OcsFrameworkAkkaSerializable
object EditorError {

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

  sealed trait PauseError                   extends EditorError
  final case class PauseFailed(msg: String) extends PauseError

  sealed trait UpdateError                                              extends EditorError
  final case class UpdateNotSupported(from: StepStatus, to: StepStatus) extends UpdateError

  sealed trait AddError              extends EditorError
  sealed trait AddBreakpointError    extends EditorError
  sealed trait ResumeError           extends EditorError
  sealed trait PrependError          extends EditorError
  sealed trait ResetError            extends EditorError
  sealed trait InsertError           extends EditorError
  sealed trait ReplaceError          extends EditorError
  sealed trait DeleteError           extends EditorError
  sealed trait RemoveBreakpointError extends EditorError

}

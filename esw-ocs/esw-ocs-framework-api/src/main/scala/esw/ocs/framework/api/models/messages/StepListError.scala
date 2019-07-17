package esw.ocs.framework.api.models.messages

import csw.params.core.models.Id
import esw.ocs.framework.api.models.StepStatus
import esw.ocs.framework.api.models.serializer.OcsFrameworkSerializable

sealed trait StepListError extends Product with Serializable

object StepListError {
  sealed trait StepListErrorSerializable extends OcsFrameworkSerializable

  case class NotSupported(stepStatus: StepStatus)
      extends InsertError
      with ReplaceError
      with DeleteError
      with AddBreakpointError
      with PauseError
      with StepListErrorSerializable

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
      with StepListErrorSerializable

  final case class IdDoesNotExist(id: Id)
      extends ReplaceError
      with InsertError
      with UpdateError
      with DeleteError
      with AddBreakpointError
      with RemoveBreakpointError
      with StepListErrorSerializable

  sealed trait AddBreakpointError extends StepListError
  sealed trait PauseError         extends StepListError
  case object PauseFailed         extends PauseError with StepListErrorSerializable

  case class AddingBreakpointNotSupported(status: StepStatus)
      extends AddBreakpointError
      with PauseError
      with StepListErrorSerializable

  sealed trait ResumeError extends StepListError

  sealed trait UpdateError                                        extends StepListError
  case class UpdateNotSupported(from: StepStatus, to: StepStatus) extends UpdateError with StepListErrorSerializable

  sealed trait AddError extends StepListError
  case object AddFailed extends AddError with StepListErrorSerializable

  sealed trait PrependError extends StepListError

  sealed trait ResetError extends StepListError

  sealed trait InsertError extends StepListError

  sealed trait ReplaceError extends StepListError

  sealed trait DeleteError extends StepListError

  sealed trait RemoveBreakpointError extends StepListError

}

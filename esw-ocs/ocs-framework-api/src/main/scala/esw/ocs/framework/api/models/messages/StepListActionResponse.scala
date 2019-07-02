package esw.ocs.framework.api.models.messages

import csw.params.core.models.Id
import esw.ocs.framework.api.models.StepStatus

sealed trait StepListActionResponse extends Product with Serializable

object StepListActionResponse {

  case object NotAllowedOnFinishedSeq
      extends AddBreakpointError
      with PauseError
      with UpdateError
      with AddError
      with ResumeError
      with DiscardPendingError
      with ReplaceError
      with PrependError
      with DeleteError
      with InsertError
      with RemoveBreakpointError

  final case class IdDoesNotExist(id: Id) extends ReplaceError with InsertError with UpdateError

  sealed trait AddBreakpointError extends StepListActionResponse
  sealed trait PauseError         extends StepListActionResponse
  case object PauseFailed         extends PauseError

  case class AddingBreakpointNotSupported(status: StepStatus) extends AddBreakpointError with PauseError

  sealed trait ResumeError extends StepListActionResponse

  sealed trait UpdateError                                        extends StepListActionResponse
  case class UpdateNotSupported(from: StepStatus, to: StepStatus) extends UpdateError

  sealed trait AddError extends StepListActionResponse
  case object AddFailed extends AddError

  sealed trait PrependError extends StepListActionResponse

  sealed trait DiscardPendingError extends StepListActionResponse

  sealed trait ReplaceError                                extends StepListActionResponse
  final case class ReplaceNotSupported(status: StepStatus) extends ReplaceError

  sealed trait DeleteError                          extends StepListActionResponse
  case class DeleteNotSupported(status: StepStatus) extends DeleteError

  sealed trait InsertError extends StepListActionResponse

  sealed trait RemoveBreakpointError extends StepListActionResponse

}

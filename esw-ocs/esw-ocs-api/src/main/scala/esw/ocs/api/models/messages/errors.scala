package esw.ocs.api.models.messages

import csw.params.core.models.Id
import esw.ocs.api.models.StepStatus
import esw.ocs.api.serializer.OcsFrameworkAkkaSerializable

final case class RegistrationError(msg: String) extends OcsFrameworkAkkaSerializable

// Lifecycle Errors

sealed trait LifecycleError extends OcsFrameworkAkkaSerializable

// Editor Errors
sealed trait EditorError extends OcsFrameworkAkkaSerializable
object EditorError {

  final case class NotSupported(stepStatus: StepStatus)
      extends InsertError
      with ReplaceError
      with DeleteError
      with AddBreakpointError
      with PauseError

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

  sealed trait AddBreakpointError    extends EditorError
  sealed trait InsertError           extends EditorError
  sealed trait ReplaceError          extends EditorError
  sealed trait DeleteError           extends EditorError
  sealed trait RemoveBreakpointError extends EditorError

}

// sequence errors

sealed trait SequenceError extends OcsFrameworkAkkaSerializable {
  val description: String
}
object SequenceError {
  case object DuplicateIdsFound extends SequenceError {
    val description = "Duplicate command Ids found in given sequence"
  }
}

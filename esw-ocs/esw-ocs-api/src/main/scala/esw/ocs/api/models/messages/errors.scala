package esw.ocs.api.models.messages

import csw.params.core.models.Id
import esw.ocs.api.serializer.OcsFrameworkAkkaSerializable

final case class RegistrationError(msg: String) extends OcsFrameworkAkkaSerializable

// Editor Errors
sealed trait EditorError extends OcsFrameworkAkkaSerializable

object EditorError {
  sealed trait AddBreakpointError extends EditorError
  sealed trait InsertError        extends EditorError
  sealed trait ReplaceError       extends EditorError
  sealed trait DeleteError        extends EditorError

  case object CannotOperateOnAnInFlightOrFinishedStep extends AddBreakpointError with DeleteError
  case object CannotInsertOrReplaceAfterAFinishedStep extends InsertError with ReplaceError
  case class IdDoesNotExist(id: Id)                   extends ReplaceError with InsertError with DeleteError with AddBreakpointError
}

// load-and-start sequence error
case object DuplicateIdsFound {
  val description = "Duplicate command Ids found in given sequence"
}

package esw.ocs.framework.api.models.messages

import csw.params.core.models.Id
import esw.ocs.framework.api.models.StepStatus

sealed trait StepListActionResponse

object StepListActionResponse {

  case object NotAllowedOnFinishedSeq
      extends AddBreakpointsResponse
      with PauseResponse
      with UpdateResponse
      with AppendResponse
      with ResumeResponse
      with DiscardPendingResponse
      with ReplaceResponse
      with PrependResponse
      with DeleteResponse
      with InsertAfterResponse
      with RemoveBreakpointsResponse

  final case class IdDoesNotExist(id: Id) extends ReplaceResponse with InsertAfterResponse with UpdateResponse

  sealed trait AddBreakpointsResponse                            extends StepListActionResponse
  case class AdditionResult(added: List[Id], notAdded: List[Id]) extends AddBreakpointsResponse

  sealed trait PauseResponse extends StepListActionResponse
  case object Paused         extends PauseResponse
  case object PauseFailed    extends PauseResponse

  sealed trait ResumeResponse extends StepListActionResponse
  case object Resumed         extends ResumeResponse

  sealed trait UpdateResponse extends StepListActionResponse
  case object Updated         extends UpdateResponse
  case object UpdateFailed    extends UpdateResponse

  sealed trait AppendResponse extends StepListActionResponse
  case object Appended        extends AppendResponse
  case object AppendFailed    extends AppendResponse

  sealed trait PrependResponse extends StepListActionResponse
  case object Prepended        extends PrependResponse

  sealed trait DiscardPendingResponse extends StepListActionResponse
  case object Discarded               extends DiscardPendingResponse

  sealed trait ReplaceResponse                                                 extends StepListActionResponse
  case object Replaced                                                         extends ReplaceResponse
  final case class ReplaceNotSupportedInThisStatus(id: Id, status: StepStatus) extends ReplaceResponse

  sealed trait DeleteResponse                                        extends StepListActionResponse
  case class DeletionResult(deleted: List[Id], notDeleted: List[Id]) extends DeleteResponse

  sealed trait InsertAfterResponse extends StepListActionResponse
  case object Inserted             extends InsertAfterResponse

  sealed trait RemoveBreakpointsResponse extends StepListActionResponse
  case object BreakpointsRemoved         extends RemoveBreakpointsResponse

}

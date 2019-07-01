package esw.ocs.framework.api.models.messages

import csw.params.core.models.Id
import esw.ocs.framework.api.models.{Step, StepStatus}

sealed trait StepListActionResponse extends Product with Serializable

object StepListActionResponse {

  case object NotAllowedOnFinishedSeq
      extends AddBreakpointResponse
      with PauseResponse
      with UpdateResponse
      with AddResponse
      with ResumeResponse
      with DiscardPendingResponse
      with ReplaceResponse
      with PrependResponse
      with DeleteResponse
      with InsertAfterResponse
      with RemoveBreakpointsResponse

  final case class IdDoesNotExist(id: Id) extends ReplaceResponse with InsertAfterResponse with UpdateResponse

  sealed trait AddBreakpointResponse                                  extends StepListActionResponse
  case object BreakpointAdded                                         extends AddBreakpointResponse
  case class AddingBreakpointNotSupportedInStatus(status: StepStatus) extends AddBreakpointResponse

  sealed trait PauseResponse extends StepListActionResponse
  case object Paused         extends PauseResponse
  case object PauseFailed    extends PauseResponse

  sealed trait ResumeResponse extends StepListActionResponse
  case object Resumed         extends ResumeResponse

  sealed trait UpdateResponse          extends StepListActionResponse
  final case class Updated(step: Step) extends UpdateResponse
  case object UpdateFailed             extends UpdateResponse

  sealed trait AddResponse extends StepListActionResponse
  case object Added        extends AddResponse
  case object AddFailed    extends AddResponse

  sealed trait PrependResponse extends StepListActionResponse
  case object Prepended        extends PrependResponse

  sealed trait DiscardPendingResponse extends StepListActionResponse
  case object Discarded               extends DiscardPendingResponse

  sealed trait ReplaceResponse                                     extends StepListActionResponse
  case object Replaced                                             extends ReplaceResponse
  final case class ReplaceNotSupportedInStatus(status: StepStatus) extends ReplaceResponse

  sealed trait DeleteResponse                               extends StepListActionResponse
  case object Deleted                                       extends DeleteResponse
  case class DeleteNotSupportedInStatus(status: StepStatus) extends DeleteResponse

  sealed trait InsertAfterResponse extends StepListActionResponse
  case object Inserted             extends InsertAfterResponse

  sealed trait RemoveBreakpointsResponse extends StepListActionResponse
  case object BreakpointRemoved          extends RemoveBreakpointsResponse

}

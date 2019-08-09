package esw.ocs.api.models.messages

import esw.ocs.api.models.{Step, StepList}

sealed trait AddResponse
sealed trait PrependResponse
sealed trait ReplaceResponse
sealed trait InsertAfterResponse
sealed trait DeleteResponse
sealed trait AddBreakpointResponse
sealed trait RemoveBreakpointResponse
sealed trait PauseResponse
sealed trait ResumeResponse
sealed trait ResetResponse
sealed trait UpdateSequencerStateResponse
sealed trait GoIdleResponse
sealed trait LoadSequenceResponse
sealed trait LoadAndProcessResponse
sealed trait StartSequenceResponse
sealed trait PullNextResponse
sealed trait MaybeNextResponse
sealed trait ReadyToExecuteNextResponse
sealed trait UpdateFailureResponse
sealed trait GoOnlineResponse
sealed trait GoOfflineResponse
sealed trait ShutdownResponse
sealed trait AbortResponse
sealed trait GetSequenceResponse
sealed trait GetPreviousSequenceResponse

case object Ok
    extends AddResponse
    with PrependResponse
    with ReplaceResponse
    with InsertAfterResponse
    with DeleteResponse
    with AddBreakpointResponse
    with RemoveBreakpointResponse
    with PauseResponse
    with ResumeResponse
    with ResetResponse
    with LoadSequenceResponse
    with StartSequenceResponse
    with ReadyToExecuteNextResponse
    with GoOnlineResponse
    with GoOfflineResponse
    with ShutdownResponse
    with AbortResponse

case class GetSequenceResult(stepList: StepList)                 extends GetSequenceResponse
case class GetPreviousSequenceResult(stepList: Option[StepList]) extends GetPreviousSequenceResponse
case class PullNextResult(step: Step)                            extends PullNextResponse
case class MaybeNextResult(step: Option[Step])                   extends MaybeNextResponse

sealed case class Unhandled(state: String, messageType: String)
    extends AddResponse
    with PrependResponse
    with ReplaceResponse
    with InsertAfterResponse
    with DeleteResponse
    with AddBreakpointResponse
    with RemoveBreakpointResponse
    with PauseResponse
    with ResumeResponse
    with ResetResponse
    with UpdateSequencerStateResponse
    with GoIdleResponse
    with LoadSequenceResponse
    with LoadAndProcessResponse
    with StartSequenceResponse
    with PullNextResponse
    with MaybeNextResponse
    with ReadyToExecuteNextResponse
    with UpdateFailureResponse
    with GoOnlineResponse
    with GoOfflineResponse
    with ShutdownResponse
    with AbortResponse
    with GetSequenceResponse
    with GetPreviousSequenceResponse {
  val description = s"Sequencer can not accept '$messageType' message in '$state' state"
}

// load-and-start sequence error
case object DuplicateIdsFound extends LoadSequenceResponse with LoadAndProcessResponse {
  val description = "Duplicate command Ids found in given sequence"
}

//object SequencerResponses {
//  final case class LoadSequenceResponse(response: Either[DuplicateIdsFound.type, Done]) extends OcsFrameworkAkkaSerializable
//  final case class EditorResponse(response: Either[EditorError, Done])                  extends OcsFrameworkAkkaSerializable
//  final case class LifecycleResponse(response: Done)                                    extends OcsFrameworkAkkaSerializable
//  final case class StepListResponse(response: Option[StepList])                         extends OcsFrameworkAkkaSerializable
//}
//trait AddResponse
//trait AddSuccess extends AddResponse
//case class Result(value: StepList) extends AddSuccess
//case class Error1(value: String)         extends AddResponse
//case class Error2(value: String)               extends AddResponse
//
//case object Unhandled extends AddResponse

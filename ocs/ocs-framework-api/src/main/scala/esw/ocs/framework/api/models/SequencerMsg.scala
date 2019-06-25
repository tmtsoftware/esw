package esw.ocs.framework.api.models

import akka.Done
import akka.actor.typed.ActorRef
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import csw.serializable.TMTSerializable

import scala.util.Try

sealed trait SupervisorMsg extends TMTSerializable

object SupervisorMsg {
  case class Shutdown(replyTo: ActorRef[Try[Unit]]) extends SupervisorMsg
  case class Abort(replyTo: ActorRef[Try[Unit]])    extends SupervisorMsg
}

sealed trait SequencerMsg extends TMTSerializable

object SequencerMsg {
  sealed trait InternalSequencerMsg extends SequencerMsg

  case class GetNext(replyTo: ActorRef[Step])            extends InternalSequencerMsg
  case class MaybeNext(replyTo: ActorRef[Option[Step]])  extends InternalSequencerMsg
  case class ReadyToExecuteNext(replyTo: ActorRef[Done]) extends InternalSequencerMsg

  sealed trait ExternalSequencerMsg[T] extends SequencerMsg with SupervisorMsg {
    def replyTo: ActorRef[T]
  }

  case class Update(submitResponse: SubmitResponse, replyTo: ActorRef[UpdateResponse]) extends InternalSequencerMsg
  case class ProcessSequence(sequence: Sequence, replyTo: ActorRef[SubmitResponse])    extends ExternalSequencerMsg[SubmitResponse]
  case class Add(commands: List[SequenceCommand], replyTo: ActorRef[AddResponse])      extends ExternalSequencerMsg[AddResponse]
  case class Pause(replyTo: ActorRef[PauseResponse])                                   extends ExternalSequencerMsg[PauseResponse]
  case class Resume(replyTo: ActorRef[ResumeResponse])                                 extends ExternalSequencerMsg[ResumeResponse]
  case class DiscardPending(replyTo: ActorRef[DiscardPendingResponse])                 extends ExternalSequencerMsg[DiscardPendingResponse]
  case class Replace(id: Id, commands: List[SequenceCommand], replyTo: ActorRef[ReplaceResponse])
      extends ExternalSequencerMsg[ReplaceResponse]
  case class Prepend(commands: List[SequenceCommand], replyTo: ActorRef[PrependResponse])
      extends ExternalSequencerMsg[PrependResponse]
  case class Delete(ids: List[Id], replyTo: ActorRef[DeleteResponse]) extends ExternalSequencerMsg[DeleteResponse]
  case class InsertAfter(id: Id, commands: List[SequenceCommand], replyTo: ActorRef[InsertAfterResponse])
      extends ExternalSequencerMsg[InsertAfterResponse]
  case class AddBreakpoints(ids: List[Id], replyTo: ActorRef[AddBreakpointsResponse])
      extends ExternalSequencerMsg[AddBreakpointsResponse]
  case class RemoveBreakpoints(ids: List[Id], replyTo: ActorRef[RemoveBreakpointsResponse])
      extends ExternalSequencerMsg[RemoveBreakpointsResponse]
  case class GetSequence(replyTo: ActorRef[StepList]) extends ExternalSequencerMsg[StepList]
}

sealed trait StepListActionResponse

sealed trait BaseResponse extends StepListActionResponse
case object NotAllowedOnFinishedSeq
    extends BaseResponse
    with AddBreakpointsResponse
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

case object Completed extends BaseResponse

sealed trait AddBreakpointsResponse                            extends StepListActionResponse
case class AdditionResult(added: List[Id], notAdded: List[Id]) extends AddBreakpointsResponse

sealed trait PauseResponse extends StepListActionResponse
case object Paused         extends PauseResponse
case object PauseFailed    extends PauseResponse

sealed trait UpdateResponse extends BaseResponse
case object Updated         extends UpdateResponse
case object UpdateFailed    extends UpdateResponse

sealed trait AddResponse extends StepListActionResponse
case object Added        extends AddResponse
case object AddFailed    extends AddResponse

sealed trait ResumeResponse extends StepListActionResponse
case object Resumed         extends ResumeResponse

sealed trait DiscardPendingResponse extends StepListActionResponse
case object Discarded               extends DiscardPendingResponse

sealed trait ReplaceResponse extends StepListActionResponse
case object Replaced         extends ReplaceResponse

sealed trait PrependResponse extends StepListActionResponse
case object Prepended        extends PrependResponse

sealed trait DeleteResponse extends StepListActionResponse
case object Deleted         extends DeleteResponse

sealed trait InsertAfterResponse extends StepListActionResponse
case object Inserted             extends InsertAfterResponse

sealed trait RemoveBreakpointsResponse extends StepListActionResponse
case object BreakpointsRemoved         extends RemoveBreakpointsResponse

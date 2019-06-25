package esw.ocs.framework.api.models

import akka.Done
import akka.actor.typed.ActorRef
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import csw.serializable.TMTSerializable
import esw.ocs.framework.api.models.StepListActionResponse._

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

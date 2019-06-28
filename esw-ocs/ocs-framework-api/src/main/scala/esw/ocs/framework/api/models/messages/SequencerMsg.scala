package esw.ocs.framework.api.models.messages

import akka.Done
import akka.actor.typed.ActorRef
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import csw.serializable.TMTSerializable
import esw.ocs.framework.api.models.messages.StepListActionResponse._
import esw.ocs.framework.api.models.{Sequence, Step, StepList}

import scala.util.Try

sealed trait SupervisorMsg extends TMTSerializable

object SupervisorMsg {
  final case class Shutdown(replyTo: ActorRef[Try[Unit]]) extends SupervisorMsg
  final case class Abort(replyTo: ActorRef[Try[Unit]])    extends SupervisorMsg
}

sealed trait SequencerMsg extends TMTSerializable

object SequencerMsg {
  sealed trait InternalSequencerMsg extends SequencerMsg

  final case class GetNext(replyTo: ActorRef[Step])            extends InternalSequencerMsg
  final case class MaybeNext(replyTo: ActorRef[Option[Step]])  extends InternalSequencerMsg
  final case class ReadyToExecuteNext(replyTo: ActorRef[Done]) extends InternalSequencerMsg

  sealed trait ExternalSequencerMsg[T] extends SequencerMsg with SupervisorMsg {
    def replyTo: ActorRef[T]
  }

  final case class Update(submitResponse: SubmitResponse, replyTo: ActorRef[UpdateResponse]) extends InternalSequencerMsg
  final case class ProcessSequence(sequence: Sequence, replyTo: ActorRef[SubmitResponse])
      extends ExternalSequencerMsg[SubmitResponse]
  final case class Add(commands: List[SequenceCommand], replyTo: ActorRef[AppendResponse])
      extends ExternalSequencerMsg[AppendResponse]
  final case class Pause(replyTo: ActorRef[PauseResponse])                   extends ExternalSequencerMsg[PauseResponse]
  final case class Resume(replyTo: ActorRef[ResumeResponse])                 extends ExternalSequencerMsg[ResumeResponse]
  final case class DiscardPending(replyTo: ActorRef[DiscardPendingResponse]) extends ExternalSequencerMsg[DiscardPendingResponse]
  final case class Replace(id: Id, commands: List[SequenceCommand], replyTo: ActorRef[ReplaceResponse])
      extends ExternalSequencerMsg[ReplaceResponse]
  final case class Prepend(commands: List[SequenceCommand], replyTo: ActorRef[PrependResponse])
      extends ExternalSequencerMsg[PrependResponse]
  final case class Delete(ids: List[Id], replyTo: ActorRef[DeleteResponse]) extends ExternalSequencerMsg[DeleteResponse]
  final case class InsertAfter(id: Id, commands: List[SequenceCommand], replyTo: ActorRef[InsertAfterResponse])
      extends ExternalSequencerMsg[InsertAfterResponse]
  final case class AddBreakpoints(ids: List[Id], replyTo: ActorRef[AddBreakpointsResponse])
      extends ExternalSequencerMsg[AddBreakpointsResponse]
  final case class RemoveBreakpoints(ids: List[Id], replyTo: ActorRef[RemoveBreakpointsResponse])
      extends ExternalSequencerMsg[RemoveBreakpointsResponse]
  final case class GetSequence(replyTo: ActorRef[StepList]) extends ExternalSequencerMsg[StepList]
}

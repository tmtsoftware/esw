package esw.ocs.framework.api.models.messages

import akka.Done
import akka.actor.typed.ActorRef
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import csw.serializable.TMTSerializable
import esw.ocs.framework.api.models.messages.StepListError._
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

  sealed trait ProcessSequenceError
  case object DuplicateIdsFound           extends ProcessSequenceError
  case object ExistingSequenceIsInProcess extends ProcessSequenceError

  final case class Update(submitResponse: SubmitResponse) extends InternalSequencerMsg
  final case class ProcessSequence(sequence: Sequence, replyTo: ActorRef[Either[ProcessSequenceError, SubmitResponse]])
      extends ExternalSequencerMsg[Either[DuplicateIdsFound.type, SubmitResponse]]

  final case class Add(commands: List[SequenceCommand], replyTo: ActorRef[AddError]) extends ExternalSequencerMsg[AddError]
  final case class Pause(replyTo: ActorRef[PauseError])                              extends ExternalSequencerMsg[PauseError]
  final case class Resume(replyTo: ActorRef[ResumeError])                            extends ExternalSequencerMsg[ResumeError]
  final case class DiscardPending(replyTo: ActorRef[DiscardPendingError])            extends ExternalSequencerMsg[DiscardPendingError]
  final case class Replace(id: Id, commands: List[SequenceCommand], replyTo: ActorRef[ReplaceError])
      extends ExternalSequencerMsg[ReplaceError]
  final case class Prepend(commands: List[SequenceCommand], replyTo: ActorRef[PrependError])
      extends ExternalSequencerMsg[PrependError]
  final case class Delete(ids: List[Id], replyTo: ActorRef[DeleteError]) extends ExternalSequencerMsg[DeleteError]
  final case class InsertAfter(id: Id, commands: List[SequenceCommand], replyTo: ActorRef[InsertError])
      extends ExternalSequencerMsg[InsertError]
  final case class AddBreakpoints(ids: List[Id], replyTo: ActorRef[AddBreakpointError])
      extends ExternalSequencerMsg[AddBreakpointError]
  final case class RemoveBreakpoints(ids: List[Id], replyTo: ActorRef[RemoveBreakpointError])
      extends ExternalSequencerMsg[RemoveBreakpointError]
  final case class GetSequence(replyTo: ActorRef[StepList]) extends ExternalSequencerMsg[StepList]
}

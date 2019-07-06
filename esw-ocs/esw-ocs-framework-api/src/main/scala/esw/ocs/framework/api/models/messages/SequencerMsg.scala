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

sealed trait SequencerMsg extends TMTSerializable

object SequencerMsg {
  sealed trait InternalSequencerMsg extends SequencerMsg

  final case class PullNext(replyTo: ActorRef[Step])           extends InternalSequencerMsg
  final case class MaybeNext(replyTo: ActorRef[Option[Step]])  extends InternalSequencerMsg
  final case class ReadyToExecuteNext(replyTo: ActorRef[Done]) extends InternalSequencerMsg

  sealed trait ExternalSequencerMsg[T] extends SequencerMsg {
    def replyTo: ActorRef[T]
  }

  final case class Shutdown(replyTo: ActorRef[Try[Unit]]) extends ExternalSequencerMsg[Try[Unit]]
  final case class Abort(replyTo: ActorRef[Try[Unit]])    extends ExternalSequencerMsg[Try[Unit]]

  final case class ProcessSequence(sequence: Sequence, replyTo: ActorRef[Either[ProcessSequenceError, SubmitResponse]])
      extends ExternalSequencerMsg[Either[ProcessSequenceError, SubmitResponse]]

  final case class GetSequence(replyTo: ActorRef[StepList]) extends ExternalSequencerMsg[StepList]
  final case class Add(commands: List[SequenceCommand], replyTo: ActorRef[Either[AddError, Done]])
      extends ExternalSequencerMsg[Either[AddError, Done]]
  final case class Pause(replyTo: ActorRef[Either[PauseError, Done]])   extends ExternalSequencerMsg[Either[PauseError, Done]]
  final case class Resume(replyTo: ActorRef[Either[ResumeError, Done]]) extends ExternalSequencerMsg[Either[ResumeError, Done]]
  final case class DiscardPending(replyTo: ActorRef[Either[DiscardPendingError, Done]])
      extends ExternalSequencerMsg[Either[DiscardPendingError, Done]]
  final case class Replace(id: Id, commands: List[SequenceCommand], replyTo: ActorRef[Either[ReplaceError, Done]])
      extends ExternalSequencerMsg[Either[ReplaceError, Done]]
  final case class Prepend(commands: List[SequenceCommand], replyTo: ActorRef[Either[PrependError, Done]])
      extends ExternalSequencerMsg[Either[PrependError, Done]]
  final case class Delete(ids: Id, replyTo: ActorRef[Either[DeleteError, Done]])
      extends ExternalSequencerMsg[Either[DeleteError, Done]]
  final case class InsertAfter(id: Id, commands: List[SequenceCommand], replyTo: ActorRef[Either[InsertError, Done]])
      extends ExternalSequencerMsg[Either[InsertError, Done]]
  final case class AddBreakpoint(id: Id, replyTo: ActorRef[Either[AddBreakpointError, Done]])
      extends ExternalSequencerMsg[Either[AddBreakpointError, Done]]
  final case class RemoveBreakpoint(id: Id, replyTo: ActorRef[Either[RemoveBreakpointError, Done]])
      extends ExternalSequencerMsg[Either[RemoveBreakpointError, Done]]
}

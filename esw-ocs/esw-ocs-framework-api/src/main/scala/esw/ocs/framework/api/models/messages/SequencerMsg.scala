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

  sealed trait ExternalSequencerMsg                       extends SequencerMsg
  final case class Shutdown(replyTo: ActorRef[Try[Unit]]) extends ExternalSequencerMsg
  final case class Abort(replyTo: ActorRef[Try[Unit]])    extends ExternalSequencerMsg

  final case class ProcessSequence(sequence: Sequence, replyTo: ActorRef[Either[ProcessSequenceError, SubmitResponse]])
      extends ExternalSequencerMsg

  final case class Available(replyTo: ActorRef[Boolean])                                           extends ExternalSequencerMsg
  final case class GetSequence(replyTo: ActorRef[StepList])                                        extends ExternalSequencerMsg
  final case class Add(commands: List[SequenceCommand], replyTo: ActorRef[Either[AddError, Done]]) extends ExternalSequencerMsg
  final case class Pause(replyTo: ActorRef[Either[PauseError, Done]])                              extends ExternalSequencerMsg
  final case class Resume(replyTo: ActorRef[Either[ResumeError, Done]])                            extends ExternalSequencerMsg
  final case class Reset(replyTo: ActorRef[Either[ResetError, Done]])                              extends ExternalSequencerMsg
  final case class Replace(id: Id, commands: List[SequenceCommand], replyTo: ActorRef[Either[ReplaceError, Done]])
      extends ExternalSequencerMsg
  final case class Prepend(commands: List[SequenceCommand], replyTo: ActorRef[Either[PrependError, Done]])
      extends ExternalSequencerMsg
  final case class Delete(ids: Id, replyTo: ActorRef[Either[DeleteError, Done]]) extends ExternalSequencerMsg
  final case class InsertAfter(id: Id, commands: List[SequenceCommand], replyTo: ActorRef[Either[InsertError, Done]])
      extends ExternalSequencerMsg
  final case class AddBreakpoint(id: Id, replyTo: ActorRef[Either[AddBreakpointError, Done]])       extends ExternalSequencerMsg
  final case class RemoveBreakpoint(id: Id, replyTo: ActorRef[Either[RemoveBreakpointError, Done]]) extends ExternalSequencerMsg
}

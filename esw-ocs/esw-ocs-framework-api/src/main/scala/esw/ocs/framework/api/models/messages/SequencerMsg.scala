package esw.ocs.framework.api.models.messages

import akka.Done
import akka.actor.typed.ActorRef
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import csw.serializable.TMTSerializable
import esw.ocs.framework.api.models.SequenceEditor.EditorResponse
import esw.ocs.framework.api.models.messages.StepListError._
import esw.ocs.framework.api.models.{Sequence, Step, StepList}

import scala.util.Try

sealed trait SequencerMsg extends TMTSerializable

object SequencerMsg {
  sealed trait InternalSequencerMsg extends SequencerMsg

  // engine msgs
  final case class PullNext(replyTo: ActorRef[Step])              extends InternalSequencerMsg
  final case class MaybeNext(replyTo: ActorRef[Option[Step]])     extends InternalSequencerMsg
  final case class ReadyToExecuteNext(replyTo: ActorRef[Done])    extends InternalSequencerMsg
  final case class UpdateFailure(failureResponse: SubmitResponse) extends InternalSequencerMsg

  sealed trait ExternalSequencerMsg extends SequencerMsg
  final case class ProcessSequence(sequence: Sequence, replyTo: ActorRef[Either[ProcessSequenceError, SubmitResponse]])
      extends ExternalSequencerMsg

  // lifecycle msgs
  final case class Shutdown(replyTo: ActorRef[Try[Unit]]) extends ExternalSequencerMsg
  final case class Abort(replyTo: ActorRef[Try[Unit]])    extends ExternalSequencerMsg

  // editor msgs
  final case class Available(replyTo: ActorRef[Boolean])                                             extends ExternalSequencerMsg
  final case class GetSequence(replyTo: ActorRef[StepList])                                          extends ExternalSequencerMsg
  final case class GetPreviousSequence(replyTo: ActorRef[Option[StepList]])                          extends ExternalSequencerMsg
  final case class Add(commands: List[SequenceCommand], replyTo: ActorRef[EditorResponse[AddError]]) extends ExternalSequencerMsg
  final case class Prepend(commands: List[SequenceCommand], replyTo: ActorRef[EditorResponse[PrependError]])
      extends ExternalSequencerMsg
  final case class Replace(id: Id, commands: List[SequenceCommand], replyTo: ActorRef[EditorResponse[ReplaceError]])
      extends ExternalSequencerMsg
  final case class InsertAfter(id: Id, commands: List[SequenceCommand], replyTo: ActorRef[EditorResponse[InsertError]])
      extends ExternalSequencerMsg
  final case class Delete(ids: Id, replyTo: ActorRef[EditorResponse[DeleteError]])                    extends ExternalSequencerMsg
  final case class AddBreakpoint(id: Id, replyTo: ActorRef[EditorResponse[AddBreakpointError]])       extends ExternalSequencerMsg
  final case class RemoveBreakpoint(id: Id, replyTo: ActorRef[EditorResponse[RemoveBreakpointError]]) extends ExternalSequencerMsg
  final case class Pause(replyTo: ActorRef[EditorResponse[PauseError]])                               extends ExternalSequencerMsg
  final case class Resume(replyTo: ActorRef[EditorResponse[ResumeError]])                             extends ExternalSequencerMsg
  final case class Reset(replyTo: ActorRef[EditorResponse[ResetError]])                               extends ExternalSequencerMsg
}

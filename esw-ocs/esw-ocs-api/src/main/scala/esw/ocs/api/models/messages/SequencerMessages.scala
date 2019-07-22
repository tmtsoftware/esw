package esw.ocs.api.models.messages

import akka.Done
import akka.actor.typed.ActorRef
import csw.command.client.messages.SequencerMsg
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.ocs.api.SequenceEditor.EditorResponse
import esw.ocs.api.models.messages.error.StepListError._
import esw.ocs.api.models.messages.error.{SequencerAbortError, SequencerShutdownError}
import esw.ocs.api.models.{Step, StepList}
import esw.ocs.api.serializer.OcsFrameworkAkkaSerializable

object SequencerMessages {
  sealed trait InternalSequencerMsg extends SequencerMsg

  // engine msgs
  final case class PullNext(replyTo: ActorRef[Step])              extends InternalSequencerMsg
  final case class MaybeNext(replyTo: ActorRef[Option[Step]])     extends InternalSequencerMsg
  final case class ReadyToExecuteNext(replyTo: ActorRef[Done])    extends InternalSequencerMsg
  final case class UpdateFailure(failureResponse: SubmitResponse) extends InternalSequencerMsg

  sealed trait ExternalEditorSequencerMsg extends SequencerMsg with OcsFrameworkAkkaSerializable

  // lifecycle msgs
  final case class Shutdown(replyTo: ActorRef[EditorResponse[SequencerShutdownError]]) extends ExternalEditorSequencerMsg
  final case class Abort(replyTo: ActorRef[EditorResponse[SequencerAbortError]])       extends ExternalEditorSequencerMsg

  // editor msgs
  final case class Available(replyTo: ActorRef[Boolean])                    extends ExternalEditorSequencerMsg
  final case class GetSequence(replyTo: ActorRef[StepList])                 extends ExternalEditorSequencerMsg
  final case class GetPreviousSequence(replyTo: ActorRef[Option[StepList]]) extends ExternalEditorSequencerMsg
  final case class Add(commands: List[SequenceCommand], replyTo: ActorRef[EditorResponse[AddError]])
      extends ExternalEditorSequencerMsg
  final case class Prepend(commands: List[SequenceCommand], replyTo: ActorRef[EditorResponse[PrependError]])
      extends ExternalEditorSequencerMsg
  final case class Replace(id: Id, commands: List[SequenceCommand], replyTo: ActorRef[EditorResponse[ReplaceError]])
      extends ExternalEditorSequencerMsg
  final case class InsertAfter(id: Id, commands: List[SequenceCommand], replyTo: ActorRef[EditorResponse[InsertError]])
      extends ExternalEditorSequencerMsg
  final case class Delete(ids: Id, replyTo: ActorRef[EditorResponse[DeleteError]])              extends ExternalEditorSequencerMsg
  final case class AddBreakpoint(id: Id, replyTo: ActorRef[EditorResponse[AddBreakpointError]]) extends ExternalEditorSequencerMsg
  final case class RemoveBreakpoint(id: Id, replyTo: ActorRef[EditorResponse[RemoveBreakpointError]])
      extends ExternalEditorSequencerMsg
  final case class Pause(replyTo: ActorRef[EditorResponse[PauseError]])   extends ExternalEditorSequencerMsg
  final case class Resume(replyTo: ActorRef[EditorResponse[ResumeError]]) extends ExternalEditorSequencerMsg
  final case class Reset(replyTo: ActorRef[EditorResponse[ResetError]])   extends ExternalEditorSequencerMsg
}

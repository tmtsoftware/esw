package esw.ocs.api.models.messages

import akka.Done
import akka.actor.typed.ActorRef
import csw.command.client.messages.SequencerMsg
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.ocs.api.models.{Step, StepList}
import esw.ocs.api.serializer.OcsFrameworkAkkaSerializable

object SequencerMessages {
  sealed trait InternalSequencerMsg extends SequencerMsg

  // engine msgs
  final case class PullNext(replyTo: ActorRef[Step])              extends InternalSequencerMsg
  final case class MaybeNext(replyTo: ActorRef[Option[Step]])     extends InternalSequencerMsg
  final case class ReadyToExecuteNext(replyTo: ActorRef[Done])    extends InternalSequencerMsg
  final case class UpdateFailure(failureResponse: SubmitResponse) extends InternalSequencerMsg

  // lifecycle msgs
  sealed trait LifecycleMsg extends SequencerMsg with OcsFrameworkAkkaSerializable

  final case class GoOnline(replyTo: ActorRef[LifecycleResponse])  extends LifecycleMsg
  final case class GoOffline(replyTo: ActorRef[LifecycleResponse]) extends LifecycleMsg
  final case class Shutdown(replyTo: ActorRef[LifecycleResponse])  extends LifecycleMsg
  final case class Abort(replyTo: ActorRef[LifecycleResponse])     extends LifecycleMsg

  // editor msgs
  sealed trait ExternalEditorSequencerMsg extends SequencerMsg with OcsFrameworkAkkaSerializable

  final case class Available(replyTo: ActorRef[Boolean]) extends ExternalEditorSequencerMsg
  // fixme : GetSequence and GetPreviousSequence should have replyTo StepListResponse
  final case class GetSequence(replyTo: ActorRef[StepList])                                    extends ExternalEditorSequencerMsg
  final case class GetPreviousSequence(replyTo: ActorRef[StepListResponse])                    extends ExternalEditorSequencerMsg
  final case class Add(commands: List[SequenceCommand], replyTo: ActorRef[EditorResponse])     extends ExternalEditorSequencerMsg
  final case class Prepend(commands: List[SequenceCommand], replyTo: ActorRef[EditorResponse]) extends ExternalEditorSequencerMsg
  final case class Replace(id: Id, commands: List[SequenceCommand], replyTo: ActorRef[EditorResponse])
      extends ExternalEditorSequencerMsg
  final case class InsertAfter(id: Id, commands: List[SequenceCommand], replyTo: ActorRef[EditorResponse])
      extends ExternalEditorSequencerMsg
  final case class Delete(ids: Id, replyTo: ActorRef[EditorResponse])          extends ExternalEditorSequencerMsg
  final case class AddBreakpoint(id: Id, replyTo: ActorRef[EditorResponse])    extends ExternalEditorSequencerMsg
  final case class RemoveBreakpoint(id: Id, replyTo: ActorRef[EditorResponse]) extends ExternalEditorSequencerMsg
  final case class Pause(replyTo: ActorRef[EditorResponse])                    extends ExternalEditorSequencerMsg
  final case class Resume(replyTo: ActorRef[EditorResponse])                   extends ExternalEditorSequencerMsg
  final case class Reset(replyTo: ActorRef[EditorResponse])                    extends ExternalEditorSequencerMsg
}

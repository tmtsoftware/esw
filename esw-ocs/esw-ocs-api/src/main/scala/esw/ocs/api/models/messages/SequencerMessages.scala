package esw.ocs.api.models.messages

import akka.Done
import akka.actor.typed.ActorRef
import csw.command.client.messages.sequencer.{SequenceResponse, SequencerMsg}
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.{Sequence, SequenceCommand}
import csw.params.core.models.Id
import esw.ocs.api.models.messages.SequencerResponses._
import esw.ocs.api.models.{Step, StepList}
import esw.ocs.api.serializer.OcsFrameworkAkkaSerializable

object SequencerMessages {
  sealed trait InternalSequencerMsg extends SequencerMsg

  final case class LoadSequence(sequence: Sequence, replyTo: ActorRef[LoadSequenceResponse])
      extends SequencerMsg
      with OcsFrameworkAkkaSerializable

  final case class StartSequence(replyTo: ActorRef[SequenceResponse]) extends SequencerMsg with OcsFrameworkAkkaSerializable

  // engine msgs
  final case class PullNext(replyTo: ActorRef[Step])              extends InternalSequencerMsg
  final case class MaybeNext(replyTo: ActorRef[Option[Step]])     extends InternalSequencerMsg
  final case class ReadyToExecuteNext(replyTo: ActorRef[Done])    extends InternalSequencerMsg
  final case class UpdateFailure(failureResponse: SubmitResponse) extends InternalSequencerMsg

  // lifecycle msgs
  sealed trait LifecycleMsg extends SequencerMsg with OcsFrameworkAkkaSerializable {
    def replyTo: ActorRef[LifecycleResponse]
  }

  final case class GoOnline(replyTo: ActorRef[LifecycleResponse])  extends LifecycleMsg
  final case class GoOffline(replyTo: ActorRef[LifecycleResponse]) extends LifecycleMsg
  final case class Shutdown(replyTo: ActorRef[LifecycleResponse])  extends LifecycleMsg
  final case class Abort(replyTo: ActorRef[LifecycleResponse])     extends LifecycleMsg

  // private messages used for lifecycle management
  private[ocs] final case object ChangeBehaviorToOffline extends SequencerMsg
  private[ocs] final case object ChangeBehaviorToDefault extends SequencerMsg

  // editor msgs

  // todo: remove this wrapper
  sealed trait ExternalEditorMsg extends SequencerMsg
  sealed trait EditorMsg[T] extends ExternalEditorMsg with OcsFrameworkAkkaSerializable {
    def replyTo: ActorRef[T]
  }

  final case class Available(replyTo: ActorRef[Boolean]) extends EditorMsg[Boolean]
  // fixme : GetSequence and GetPreviousSequence should have replyTo StepListResponse
  final case class GetSequence(replyTo: ActorRef[StepList])                                    extends EditorMsg[StepList]
  final case class GetPreviousSequence(replyTo: ActorRef[StepListResponse])                    extends EditorMsg[StepListResponse]
  final case class Add(commands: List[SequenceCommand], replyTo: ActorRef[EditorResponse])     extends EditorMsg[EditorResponse]
  final case class Prepend(commands: List[SequenceCommand], replyTo: ActorRef[EditorResponse]) extends EditorMsg[EditorResponse]
  final case class Replace(id: Id, commands: List[SequenceCommand], replyTo: ActorRef[EditorResponse])
      extends EditorMsg[EditorResponse]
  final case class InsertAfter(id: Id, commands: List[SequenceCommand], replyTo: ActorRef[EditorResponse])
      extends EditorMsg[EditorResponse]
  final case class Delete(ids: Id, replyTo: ActorRef[EditorResponse])          extends EditorMsg[EditorResponse]
  final case class AddBreakpoint(id: Id, replyTo: ActorRef[EditorResponse])    extends EditorMsg[EditorResponse]
  final case class RemoveBreakpoint(id: Id, replyTo: ActorRef[EditorResponse]) extends EditorMsg[EditorResponse]
  final case class Pause(replyTo: ActorRef[EditorResponse])                    extends EditorMsg[EditorResponse]
  final case class Resume(replyTo: ActorRef[EditorResponse])                   extends EditorMsg[EditorResponse]
  final case class Reset(replyTo: ActorRef[EditorResponse])                    extends EditorMsg[EditorResponse]
}

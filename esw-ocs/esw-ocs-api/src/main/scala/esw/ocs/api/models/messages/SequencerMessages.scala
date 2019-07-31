package esw.ocs.api.models.messages

import akka.Done
import akka.actor.typed.ActorRef
import csw.command.client.messages.sequencer.{SequenceResponse, SequencerMsg}
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.{Sequence, SequenceCommand}
import csw.params.core.models.Id
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
  sealed abstract class LifecycleMsg(val replyTo: ActorRef[LifecycleResponse])
      extends SequencerMsg
      with OcsFrameworkAkkaSerializable

  final case class GoOnline(override val replyTo: ActorRef[LifecycleResponse])  extends LifecycleMsg(replyTo)
  final case class GoOffline(override val replyTo: ActorRef[LifecycleResponse]) extends LifecycleMsg(replyTo)
  final case class Shutdown(override val replyTo: ActorRef[LifecycleResponse])  extends LifecycleMsg(replyTo)
  final case class Abort(override val replyTo: ActorRef[LifecycleResponse])     extends LifecycleMsg(replyTo)

  // private messages used for lifecycle management
  private[ocs] final case object ChangeBehaviorToOffline extends SequencerMsg
  private[ocs] final case object ChangeBehaviorToDefault extends SequencerMsg

  // editor msgs

  // todo: remove this wrapper
  sealed trait ExternalEditorMsg                               extends SequencerMsg
  sealed abstract class EditorMsg[T](val replyTo: ActorRef[T]) extends ExternalEditorMsg with OcsFrameworkAkkaSerializable

  final case class Available(override val replyTo: ActorRef[Boolean]) extends EditorMsg(replyTo)
  // fixme : GetSequence and GetPreviousSequence should have replyTo StepListResponse
  final case class GetSequence(override val replyTo: ActorRef[StepList])                                extends EditorMsg(replyTo)
  final case class GetPreviousSequence(override val replyTo: ActorRef[StepListResponse])                extends EditorMsg(replyTo)
  final case class Add(commands: List[SequenceCommand], override val replyTo: ActorRef[EditorResponse]) extends EditorMsg(replyTo)
  final case class Prepend(commands: List[SequenceCommand], override val replyTo: ActorRef[EditorResponse])
      extends EditorMsg(replyTo)
  final case class Replace(id: Id, commands: List[SequenceCommand], override val replyTo: ActorRef[EditorResponse])
      extends EditorMsg(replyTo)
  final case class InsertAfter(id: Id, commands: List[SequenceCommand], override val replyTo: ActorRef[EditorResponse])
      extends EditorMsg(replyTo)
  final case class Delete(ids: Id, override val replyTo: ActorRef[EditorResponse])          extends EditorMsg(replyTo)
  final case class AddBreakpoint(id: Id, override val replyTo: ActorRef[EditorResponse])    extends EditorMsg(replyTo)
  final case class RemoveBreakpoint(id: Id, override val replyTo: ActorRef[EditorResponse]) extends EditorMsg(replyTo)
  final case class Pause(override val replyTo: ActorRef[EditorResponse])                    extends EditorMsg(replyTo)
  final case class Resume(override val replyTo: ActorRef[EditorResponse])                   extends EditorMsg(replyTo)
  final case class Reset(override val replyTo: ActorRef[EditorResponse])                    extends EditorMsg(replyTo)
}

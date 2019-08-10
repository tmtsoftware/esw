package esw.ocs.api.models.messages

import akka.actor.typed.ActorRef
import csw.command.client.messages.sequencer.SequencerMsg
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.{Sequence, SequenceCommand}
import csw.params.core.models.Id
import esw.ocs.api.models.SequencerState
import esw.ocs.api.serializer.OcsFrameworkAkkaSerializable

object SequencerMessages {

  sealed trait EswSequencerMessage extends SequencerMsg with OcsFrameworkAkkaSerializable {
    def replyTo: ActorRef[Unhandled]
  }

  sealed trait IdleMessage           extends EswSequencerMessage
  sealed trait SequenceLoadedMessage extends EswSequencerMessage
  sealed trait InProgressMessage     extends EswSequencerMessage
  sealed trait OfflineMessage        extends EswSequencerMessage
  sealed trait EditorAction          extends SequenceLoadedMessage with InProgressMessage
  sealed trait AnyStateMessage       extends IdleMessage with SequenceLoadedMessage with InProgressMessage with OfflineMessage

  final case class LoadSequence(sequence: Sequence, replyTo: ActorRef[LoadSequenceResponse])   extends IdleMessage
  final case class LoadAndProcess(sequence: Sequence, replyTo: ActorRef[LoadSequenceResponse]) extends IdleMessage
  final case class StartSequence(replyTo: ActorRef[SimpleResponse])                            extends SequenceLoadedMessage

  // lifecycle msgs
  final case class GoOnline(replyTo: ActorRef[SimpleResponse])  extends OfflineMessage
  final case class GoOffline(replyTo: ActorRef[SimpleResponse]) extends IdleMessage with SequenceLoadedMessage
  final case class Shutdown(replyTo: ActorRef[SimpleResponse])  extends AnyStateMessage

  final case class Abort(replyTo: ActorRef[SimpleResponse]) extends EditorAction

  // editor msgs
  // fixme : GetSequence and GetPreviousSequence should have replyTo StepListResponse
  final case class GetSequence(replyTo: ActorRef[GetSequenceResponse])                 extends EditorAction
  final case class GetPreviousSequence(replyTo: ActorRef[GetPreviousSequenceResponse]) extends AnyStateMessage

  final case class Add(commands: List[SequenceCommand], replyTo: ActorRef[SimpleResponse])     extends EditorAction
  final case class Prepend(commands: List[SequenceCommand], replyTo: ActorRef[SimpleResponse]) extends EditorAction

  final case class Replace(id: Id, commands: List[SequenceCommand], replyTo: ActorRef[SimpleResponse])     extends EditorAction
  final case class InsertAfter(id: Id, commands: List[SequenceCommand], replyTo: ActorRef[SimpleResponse]) extends EditorAction
  final case class Delete(ids: Id, replyTo: ActorRef[SimpleResponse])                                      extends EditorAction
  final case class AddBreakpoint(id: Id, replyTo: ActorRef[SimpleResponse])                                extends EditorAction
  final case class RemoveBreakpoint(id: Id, replyTo: ActorRef[SimpleResponse])                             extends EditorAction
  final case class Pause(replyTo: ActorRef[SimpleResponse])                                                extends EditorAction
  final case class Resume(replyTo: ActorRef[SimpleResponse])                                               extends EditorAction
  final case class Reset(replyTo: ActorRef[SimpleResponse])                                                extends EditorAction

  // engine & internal
  final private[esw] case class PullNext(replyTo: ActorRef[PullNextResponse])         extends IdleMessage with InProgressMessage
  final private[esw] case class MaybeNext(replyTo: ActorRef[MaybeNextResponse])       extends InProgressMessage
  final private[esw] case class ReadyToExecuteNext(replyTo: ActorRef[SimpleResponse]) extends InProgressMessage
  final private[esw] case class UpdateFailure(failureResponse: SubmitResponse, replyTo: ActorRef[UpdateFailureResponse])
      extends InProgressMessage
  final private[esw] case class UpdateSequencerState(
      state: SequencerState,
      replyTo: ActorRef[UpdateSequencerStateResponse]
  ) extends InProgressMessage
  final private[esw] case class GoIdle(state: SequencerState, replyTo: ActorRef[SimpleResponse]) extends InProgressMessage
}

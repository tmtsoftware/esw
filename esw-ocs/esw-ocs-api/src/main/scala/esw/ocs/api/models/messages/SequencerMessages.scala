package esw.ocs.api.models.messages

import akka.actor.typed.ActorRef
import csw.command.client.messages.sequencer.SequencerMsg
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.{Sequence, SequenceCommand}
import csw.params.core.models.Id
import esw.ocs.api.serializer.OcsAkkaSerializable

object SequencerMessages {

  sealed trait EswSequencerMessage extends SequencerMsg with OcsAkkaSerializable with Product with Serializable {
    def replyTo: ActorRef[Unhandled]
  }

  sealed trait IdleMessage           extends EswSequencerMessage
  sealed trait SequenceLoadedMessage extends EswSequencerMessage
  sealed trait InProgressMessage     extends EswSequencerMessage
  sealed trait OfflineMessage        extends EswSequencerMessage
  sealed trait GoingOnlineMessage    extends EswSequencerMessage
  sealed trait GoingOfflineMessage   extends EswSequencerMessage
  sealed trait EditorAction          extends SequenceLoadedMessage with InProgressMessage
  sealed trait CommonMessage
      extends IdleMessage
      with SequenceLoadedMessage
      with InProgressMessage
      with OfflineMessage
      with GoingOnlineMessage
      with GoingOfflineMessage

  final case class LoadSequence(sequence: Sequence, replyTo: ActorRef[LoadSequenceResponse]) extends IdleMessage
  final case class StartSequence(replyTo: ActorRef[SequenceResponse])                        extends SequenceLoadedMessage

  // lifecycle msgs
  final case class GoOnline(replyTo: ActorRef[GoOnlineResponse]) extends OfflineMessage

  final case class GoOffline(replyTo: ActorRef[OkOrUnhandledResponse]) extends IdleMessage with SequenceLoadedMessage

  final case class Shutdown(replyTo: ActorRef[OkOrUnhandledResponse]) extends CommonMessage

  final case class Abort(replyTo: ActorRef[OkOrUnhandledResponse]) extends EditorAction

  // editor msgs
  final case class GetSequence(replyTo: ActorRef[StepListResponse]) extends CommonMessage

  final case class GetPreviousSequence(replyTo: ActorRef[StepListResponse]) extends CommonMessage

  final case class Add(commands: List[SequenceCommand], replyTo: ActorRef[OkOrUnhandledResponse]) extends EditorAction

  final case class Prepend(commands: List[SequenceCommand], replyTo: ActorRef[OkOrUnhandledResponse]) extends EditorAction

  final case class Replace(id: Id, commands: List[SequenceCommand], replyTo: ActorRef[GenericResponse]) extends EditorAction

  final case class InsertAfter(id: Id, commands: List[SequenceCommand], replyTo: ActorRef[GenericResponse]) extends EditorAction

  final case class Delete(ids: Id, replyTo: ActorRef[GenericResponse]) extends EditorAction

  final case class AddBreakpoint(id: Id, replyTo: ActorRef[GenericResponse]) extends EditorAction

  final case class RemoveBreakpoint(id: Id, replyTo: ActorRef[RemoveBreakpointResponse]) extends EditorAction

  final case class Pause(replyTo: ActorRef[PauseResponse]) extends EditorAction

  final case class Resume(replyTo: ActorRef[OkOrUnhandledResponse]) extends EditorAction

  final case class Reset(replyTo: ActorRef[OkOrUnhandledResponse]) extends EditorAction

  final private[ocs] case class LoadAndStartSequenceInternal(sequence: Sequence, replyTo: ActorRef[SequenceResponse])
      extends IdleMessage

  // engine & internal
  final private[esw] case class PullNext(replyTo: ActorRef[PullNextResponse])                extends IdleMessage with InProgressMessage
  final private[esw] case class MaybeNext(replyTo: ActorRef[MaybeNextResponse])              extends InProgressMessage
  final private[esw] case class ReadyToExecuteNext(replyTo: ActorRef[OkOrUnhandledResponse]) extends InProgressMessage
  final private[esw] case class Update(submitResponse: SubmitResponse, replyTo: ActorRef[OkOrUnhandledResponse]) // this is internal message and replyTo is not used anywhere
      extends InProgressMessage
  final private[esw] case class GoIdle(replyTo: ActorRef[OkOrUnhandledResponse])      extends InProgressMessage with GoingOnlineMessage
  final private[esw] case class GoneOffline(replyTo: ActorRef[OkOrUnhandledResponse]) extends GoingOfflineMessage
}

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
    def replyTo: Option[ActorRef[Unhandled]]
  }

  sealed trait IdleMessage           extends EswSequencerMessage
  sealed trait SequenceLoadedMessage extends EswSequencerMessage
  sealed trait InProgressMessage     extends EswSequencerMessage
  sealed trait OfflineMessage        extends EswSequencerMessage

  final case class LoadSequence(sequence: Sequence, replyTo: Option[ActorRef[LoadSequenceResponse]])     extends IdleMessage
  final case class LoadAndProcess(sequence: Sequence, replyTo: Option[ActorRef[LoadAndProcessResponse]]) extends IdleMessage
  final case class StartSequence(replyTo: Option[ActorRef[StartSequenceResponse]])                       extends SequenceLoadedMessage

  // lifecycle msgs
  final case class GoOnline(replyTo: Option[ActorRef[GoOnlineResponse]])   extends OfflineMessage
  final case class GoOffline(replyTo: Option[ActorRef[GoOfflineResponse]]) extends IdleMessage with SequenceLoadedMessage
  final case class Shutdown(replyTo: Option[ActorRef[ShutdownResponse]])
      extends IdleMessage
      with SequenceLoadedMessage
      with InProgressMessage
      with OfflineMessage

  final case class Abort(replyTo: Option[ActorRef[AbortResponse]]) extends SequenceLoadedMessage with InProgressMessage

  // editor msgs
  // fixme : GetSequence and GetPreviousSequence should have replyTo StepListResponse
  final case class GetSequence(replyTo: Option[ActorRef[GetSequenceResponse]])
      extends SequenceLoadedMessage
      with InProgressMessage

  final case class GetPreviousSequence(replyTo: Option[ActorRef[GetPreviousSequenceResponse]])
      extends IdleMessage
      with SequenceLoadedMessage
      with InProgressMessage
      with OfflineMessage

  final case class Add(commands: List[SequenceCommand], replyTo: Option[ActorRef[AddResponse]])
      extends SequenceLoadedMessage
      with InProgressMessage
  final case class Prepend(commands: List[SequenceCommand], replyTo: Option[ActorRef[PrependResponse]])
      extends SequenceLoadedMessage
      with InProgressMessage
  final case class Replace(id: Id, commands: List[SequenceCommand], replyTo: Option[ActorRef[ReplaceResponse]])
      extends SequenceLoadedMessage
      with InProgressMessage
  final case class InsertAfter(id: Id, commands: List[SequenceCommand], replyTo: Option[ActorRef[InsertAfterResponse]])
      extends SequenceLoadedMessage
      with InProgressMessage
  final case class Delete(ids: Id, replyTo: Option[ActorRef[DeleteResponse]]) extends SequenceLoadedMessage with InProgressMessage
  final case class AddBreakpoint(id: Id, replyTo: Option[ActorRef[AddBreakpointResponse]])
      extends SequenceLoadedMessage
      with InProgressMessage
  final case class RemoveBreakpoint(id: Id, replyTo: Option[ActorRef[RemoveBreakpointResponse]])
      extends SequenceLoadedMessage
      with InProgressMessage
  final case class Pause(replyTo: Option[ActorRef[PauseResponse]])   extends SequenceLoadedMessage with InProgressMessage
  final case class Resume(replyTo: Option[ActorRef[ResumeResponse]]) extends SequenceLoadedMessage with InProgressMessage
  final case class Reset(replyTo: Option[ActorRef[ResetResponse]])   extends SequenceLoadedMessage with InProgressMessage

  // engine & internal
  final private[esw] case class PullNext(replyTo: Option[ActorRef[PullNextResponse]])   extends IdleMessage with InProgressMessage
  final private[esw] case class MaybeNext(replyTo: Option[ActorRef[MaybeNextResponse]]) extends InProgressMessage
  final private[esw] case class ReadyToExecuteNext(replyTo: Option[ActorRef[ReadyToExecuteNextResponse]])
      extends InProgressMessage
  final private[esw] case class UpdateFailure(failureResponse: SubmitResponse, replyTo: Option[ActorRef[UpdateFailureResponse]])
      extends InProgressMessage
  final private[esw] case class UpdateSequencerState(
      state: SequencerState,
      replyTo: Option[ActorRef[UpdateSequencerStateResponse]]
  ) extends InProgressMessage
  final private[esw] case class GoIdle(state: SequencerState, replyTo: Option[ActorRef[GoIdleResponse]]) extends InProgressMessage
}

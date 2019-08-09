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

  final case class LoadSequence(sequence: Sequence, replyTo: ActorRef[LoadSequenceResponse])     extends IdleMessage
  final case class LoadAndProcess(sequence: Sequence, replyTo: ActorRef[LoadAndProcessResponse]) extends IdleMessage
  final case class StartSequence(replyTo: ActorRef[StartSequenceResponse])                       extends SequenceLoadedMessage

  // engine msgs
  final case class PullNext(replyTo: ActorRef[PullNextResponse])                     extends IdleMessage with InProgressMessage
  final case class MaybeNext(replyTo: ActorRef[MaybeNextResponse])                   extends InProgressMessage
  final case class ReadyToExecuteNext(replyTo: ActorRef[ReadyToExecuteNextResponse]) extends InProgressMessage
  final case class UpdateFailure(failureResponse: SubmitResponse, replyTo: ActorRef[UpdateFailureResponse])
      extends InProgressMessage

  // lifecycle msgs
  final case class GoOnline(replyTo: ActorRef[GoOnlineResponse])   extends OfflineMessage
  final case class GoOffline(replyTo: ActorRef[GoOfflineResponse]) extends IdleMessage with SequenceLoadedMessage
  final case class Shutdown(replyTo: ActorRef[ShutdownResponse])
      extends IdleMessage
      with SequenceLoadedMessage
      with InProgressMessage
      with OfflineMessage

  final case class Abort(replyTo: ActorRef[AbortResponse]) extends SequenceLoadedMessage with InProgressMessage

  // editor msgs
  // fixme : GetSequence and GetPreviousSequence should have replyTo StepListResponse
  final case class GetSequence(replyTo: ActorRef[GetSequenceResponse]) extends SequenceLoadedMessage with InProgressMessage

  final case class GetPreviousSequence(replyTo: ActorRef[GetPreviousSequenceResponse])
      extends IdleMessage
      with SequenceLoadedMessage
      with InProgressMessage
      with OfflineMessage

  final case class Add(commands: List[SequenceCommand], replyTo: ActorRef[AddResponse])
      extends SequenceLoadedMessage
      with InProgressMessage
  final case class Prepend(commands: List[SequenceCommand], replyTo: ActorRef[PrependResponse])
      extends SequenceLoadedMessage
      with InProgressMessage
  final case class Replace(id: Id, commands: List[SequenceCommand], replyTo: ActorRef[ReplaceResponse])
      extends SequenceLoadedMessage
      with InProgressMessage
  final case class InsertAfter(id: Id, commands: List[SequenceCommand], replyTo: ActorRef[InsertAfterResponse])
      extends SequenceLoadedMessage
      with InProgressMessage
  final case class Delete(ids: Id, replyTo: ActorRef[DeleteResponse]) extends SequenceLoadedMessage with InProgressMessage
  final case class AddBreakpoint(id: Id, replyTo: ActorRef[AddBreakpointResponse])
      extends SequenceLoadedMessage
      with InProgressMessage
  final case class RemoveBreakpoint(id: Id, replyTo: ActorRef[RemoveBreakpointResponse])
      extends SequenceLoadedMessage
      with InProgressMessage
  final case class Pause(replyTo: ActorRef[PauseResponse])   extends SequenceLoadedMessage with InProgressMessage
  final case class Resume(replyTo: ActorRef[ResumeResponse]) extends SequenceLoadedMessage with InProgressMessage
  final case class Reset(replyTo: ActorRef[ResetResponse])   extends SequenceLoadedMessage with InProgressMessage

  //internal
  final private[esw] case class UpdateSequencerState(state: SequencerState, replyTo: ActorRef[UpdateSequencerStateResponse])
      extends InProgressMessage
  final private[esw] case class GoIdle(state: SequencerState, replyTo: ActorRef[GoIdleResponse]) extends InProgressMessage
}

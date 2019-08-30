package esw.ocs.client.messages

import akka.actor.typed.ActorRef
import csw.command.client.messages.sequencer.SequencerMsg
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.{Sequence, SequenceCommand}
import csw.params.core.models.Id
import esw.ocs.api.models.codecs.OcsAkkaSerializable
import esw.ocs.api.models.responses._
import esw.ocs.api.models.{Step, StepList}

object SequencerMessages {

  sealed trait EswSequencerMessage extends SequencerMsg with OcsAkkaSerializable

  sealed trait CommonMessage       extends EswSequencerMessage
  sealed trait ShuttingDownMessage extends EswSequencerMessage
  sealed trait UnhandleableSequencerMessage extends EswSequencerMessage {
    def replyTo: ActorRef[Unhandled]
  }

  sealed trait IdleMessage           extends UnhandleableSequencerMessage
  sealed trait SequenceLoadedMessage extends UnhandleableSequencerMessage
  sealed trait InProgressMessage     extends UnhandleableSequencerMessage
  sealed trait OfflineMessage        extends UnhandleableSequencerMessage
  sealed trait GoingOnlineMessage    extends UnhandleableSequencerMessage
  sealed trait GoingOfflineMessage   extends UnhandleableSequencerMessage
  sealed trait AbortSequenceMessage  extends UnhandleableSequencerMessage
  sealed trait EditorAction          extends SequenceLoadedMessage with InProgressMessage

  // startup msgs
  final case class LoadSequence(sequence: Sequence, replyTo: ActorRef[LoadSequenceResponse])         extends IdleMessage
  final case class StartSequence(replyTo: ActorRef[OkOrUnhandledResponse])                           extends SequenceLoadedMessage
  final case class LoadAndStartSequence(sequence: Sequence, replyTo: ActorRef[LoadSequenceResponse]) extends IdleMessage
  final case class QuerySequenceResponse(replyTo: ActorRef[SequenceResponse])
      extends IdleMessage
      with SequenceLoadedMessage
      with InProgressMessage

  // common msgs
  final case class Shutdown(replyTo: ActorRef[Ok.type])                               extends CommonMessage
  final case class GetSequence(replyTo: ActorRef[Option[StepList]])                   extends CommonMessage
  final case class GetSequencerState(replyTo: ActorRef[SequencerState[SequencerMsg]]) extends CommonMessage
  final private[esw] case class ReadyToExecuteNext(replyTo: ActorRef[Ok.type])        extends CommonMessage
  final private[esw] case class MaybeNext(replyTo: ActorRef[Option[Step]])            extends CommonMessage

  // lifecycle msgs
  final case class GoOnline(replyTo: ActorRef[GoOnlineResponse])       extends OfflineMessage
  final case class GoOffline(replyTo: ActorRef[OkOrUnhandledResponse]) extends IdleMessage with SequenceLoadedMessage

  // editor msgs
  final case class Add(commands: List[SequenceCommand], replyTo: ActorRef[OkOrUnhandledResponse])           extends EditorAction
  final case class Prepend(commands: List[SequenceCommand], replyTo: ActorRef[OkOrUnhandledResponse])       extends EditorAction
  final case class Replace(id: Id, commands: List[SequenceCommand], replyTo: ActorRef[GenericResponse])     extends EditorAction
  final case class InsertAfter(id: Id, commands: List[SequenceCommand], replyTo: ActorRef[GenericResponse]) extends EditorAction
  final case class Delete(ids: Id, replyTo: ActorRef[GenericResponse])                                      extends EditorAction
  final case class AddBreakpoint(id: Id, replyTo: ActorRef[GenericResponse])                                extends EditorAction
  final case class RemoveBreakpoint(id: Id, replyTo: ActorRef[RemoveBreakpointResponse])                    extends EditorAction
  final case class Pause(replyTo: ActorRef[PauseResponse])                                                  extends EditorAction
  final case class Resume(replyTo: ActorRef[OkOrUnhandledResponse])                                         extends EditorAction
  final case class Reset(replyTo: ActorRef[OkOrUnhandledResponse])                                          extends EditorAction

  final case class AbortSequence(replyTo: ActorRef[OkOrUnhandledResponse]) extends SequenceLoadedMessage with InProgressMessage

  // engine & internal
  final private[ocs] case class LoadAndProcessSequenceInternal(sequence: Sequence, replyTo: ActorRef[SequenceResponse])
      extends IdleMessage

  final private[esw] case class PullNext(replyTo: ActorRef[PullNextResponse]) extends IdleMessage with InProgressMessage
  final private[esw] case class Update(submitResponse: SubmitResponse, replyTo: ActorRef[OkOrUnhandledResponse]) // this is internal message and replyTo is not used anywhere
      extends InProgressMessage
  final private[esw] case class GoIdle(replyTo: ActorRef[OkOrUnhandledResponse])                extends InProgressMessage
  final private[esw] case class GoneOffline(replyTo: ActorRef[OkOrUnhandledResponse])           extends GoingOfflineMessage
  final private[esw] case class GoOnlineSuccess(replyTo: ActorRef[GoOnlineResponse])            extends GoingOnlineMessage
  final private[esw] case class GoOnlineFailed(replyTo: ActorRef[GoOnlineResponse])             extends GoingOnlineMessage
  final private[esw] case class ShutdownComplete(replyTo: ActorRef[Ok.type])                    extends ShuttingDownMessage
  final private[esw] case class AbortSequenceComplete(replyTo: ActorRef[OkOrUnhandledResponse]) extends AbortSequenceMessage
}

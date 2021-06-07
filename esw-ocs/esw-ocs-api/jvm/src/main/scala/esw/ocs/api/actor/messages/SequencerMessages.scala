package esw.ocs.api.actor.messages

import akka.actor.typed.ActorRef
import csw.command.client.messages.sequencer.SequencerMsg
import csw.location.api.models.AkkaLocation
import csw.params.commands.{Sequence, SequenceCommand}
import csw.params.core.models.Id
import csw.time.core.models.UTCTime
import esw.ocs.api.codecs.OcsAkkaSerializable
import esw.ocs.api.models.{Step, StepList}
import esw.ocs.api.protocol._

object SequencerMessages {

  sealed trait EswSequencerMessage       extends SequencerMsg
  sealed trait EswSequencerRemoteMessage extends EswSequencerMessage with OcsAkkaSerializable
  // Messages which are handled in all states
  sealed trait CommonMessage       extends EswSequencerMessage
  sealed trait CommonRemoteMessage extends CommonMessage with EswSequencerRemoteMessage
  sealed trait UnhandleableSequencerMessage extends EswSequencerRemoteMessage {
    def replyTo: ActorRef[Unhandled]
  }

  // Having state specific messages enables exhaustive match (compile time safety) while handling messages in SequencerBehavior
  sealed trait IdleMessage           extends UnhandleableSequencerMessage
  sealed trait SequenceLoadedMessage extends UnhandleableSequencerMessage
  sealed trait RunningMessage        extends UnhandleableSequencerMessage
  sealed trait OfflineMessage        extends UnhandleableSequencerMessage
  sealed trait GoingOnlineMessage    extends UnhandleableSequencerMessage
  sealed trait GoingOfflineMessage   extends UnhandleableSequencerMessage
  sealed trait AbortSequenceMessage  extends UnhandleableSequencerMessage
  sealed trait StopMessage           extends UnhandleableSequencerMessage
  sealed trait SubmitMessage         extends UnhandleableSequencerMessage
  sealed trait StartingMessage       extends UnhandleableSequencerMessage
  sealed trait EditorAction          extends SequenceLoadedMessage with RunningMessage

  // startup msgs
  final case class LoadSequence(sequence: Sequence, replyTo: ActorRef[OkOrUnhandledResponse])
      extends IdleMessage
      with SequenceLoadedMessage

  final case class StartSequence(replyTo: ActorRef[SequencerSubmitResponse]) extends SequenceLoadedMessage
  final case class SubmitSequenceInternal(sequence: Sequence, replyTo: ActorRef[SequencerSubmitResponse])
      extends IdleMessage
      with SequenceLoadedMessage

  // common msgs
  final case class Shutdown(replyTo: ActorRef[Ok.type])                                              extends CommonRemoteMessage
  final case class GetSequence(replyTo: ActorRef[Option[StepList]])                                  extends CommonRemoteMessage
  final case class GetSequencerState(replyTo: ActorRef[InternalSequencerState[SequencerMsg]])        extends CommonRemoteMessage
  final case class GetSequenceComponent(replyTo: ActorRef[AkkaLocation])                             extends CommonRemoteMessage
  final case class SubscribeSequencerState(subscriber: ActorRef[SequencerStateSubscriptionResponse]) extends CommonRemoteMessage

  final private[esw] case class UnsubscribeSequencerState(subscriber: ActorRef[SequencerStateSubscriptionResponse])
      extends CommonRemoteMessage
  final private[esw] case class ReadyToExecuteNext(replyTo: ActorRef[Ok.type]) extends CommonMessage
  final private[esw] case class MaybeNext(replyTo: ActorRef[Option[Step]])     extends CommonMessage

  // diagnostic data msgs
  sealed trait DiagnosticDataMessage extends CommonRemoteMessage
  case class DiagnosticMode(startTime: UTCTime, hint: String, replyTo: ActorRef[DiagnosticModeResponse])
      extends DiagnosticDataMessage
  case class OperationsMode(replyTo: ActorRef[OperationsModeResponse]) extends DiagnosticDataMessage

  // lifecycle msgs
  sealed trait LifecycleMsg                                        extends IdleMessage with OfflineMessage with SequenceLoadedMessage
  final case class GoOnline(replyTo: ActorRef[GoOnlineResponse])   extends LifecycleMsg
  final case class GoOffline(replyTo: ActorRef[GoOfflineResponse]) extends LifecycleMsg

  // editor msgs
  final case class Add(commands: List[SequenceCommand], replyTo: ActorRef[OkOrUnhandledResponse])           extends EditorAction
  final case class Prepend(commands: List[SequenceCommand], replyTo: ActorRef[OkOrUnhandledResponse])       extends EditorAction
  final case class Replace(id: Id, commands: List[SequenceCommand], replyTo: ActorRef[GenericResponse])     extends EditorAction
  final case class InsertAfter(id: Id, commands: List[SequenceCommand], replyTo: ActorRef[GenericResponse]) extends EditorAction
  final case class Delete(ids: Id, replyTo: ActorRef[GenericResponse])                                      extends EditorAction
  final case class AddBreakpoint(id: Id, replyTo: ActorRef[GenericResponse])                                extends EditorAction
  final case class RemoveBreakpoint(id: Id, replyTo: ActorRef[RemoveBreakpointResponse])                    extends EditorAction
  final case class Reset(replyTo: ActorRef[OkOrUnhandledResponse])                                          extends EditorAction

  // running msgs
  final case class AbortSequence(replyTo: ActorRef[OkOrUnhandledResponse]) extends RunningMessage
  final case class Stop(replyTo: ActorRef[OkOrUnhandledResponse])          extends RunningMessage
  final case class Pause(replyTo: ActorRef[PauseResponse])                 extends RunningMessage
  final case class Resume(replyTo: ActorRef[OkOrUnhandledResponse])        extends RunningMessage

  // engine & internal
  final private[esw] case class PullNext(replyTo: ActorRef[PullNextResponse]) extends IdleMessage with RunningMessage
  // this is an internal message and replyTo is not used anywhere
  final private[esw] case class StepSuccess(replyTo: ActorRef[OkOrUnhandledResponse]) extends RunningMessage
  // this is an internal message and replyTo is not used anywhere
  final private[esw] case class StepFailure(reason: String, replyTo: ActorRef[OkOrUnhandledResponse]) extends RunningMessage

  final private[esw] case class GoIdle(replyTo: ActorRef[OkOrUnhandledResponse])                extends RunningMessage
  final private[esw] case class GoOfflineSuccess(replyTo: ActorRef[GoOfflineResponse])          extends GoingOfflineMessage
  final private[esw] case class GoOfflineFailed(replyTo: ActorRef[GoOfflineResponse])           extends GoingOfflineMessage
  final private[esw] case class GoOnlineSuccess(replyTo: ActorRef[GoOnlineResponse])            extends GoingOnlineMessage
  final private[esw] case class GoOnlineFailed(replyTo: ActorRef[GoOnlineResponse])             extends GoingOnlineMessage
  final private[esw] case class AbortSequenceComplete(replyTo: ActorRef[OkOrUnhandledResponse]) extends AbortSequenceMessage
  final private[esw] case class StopComplete(replyTo: ActorRef[OkOrUnhandledResponse])          extends StopMessage
  final private[esw] case class SubmitSuccessful(sequence: Sequence, replyTo: ActorRef[SequencerSubmitResponse])
      extends SubmitMessage
  final private[esw] case class SubmitFailed(replyTo: ActorRef[SequencerSubmitResponse])       extends SubmitMessage
  final private[esw] case class StartingSuccessful(replyTo: ActorRef[SequencerSubmitResponse]) extends StartingMessage
  final private[esw] case class StartingFailed(replyTo: ActorRef[SequencerSubmitResponse])     extends StartingMessage
}

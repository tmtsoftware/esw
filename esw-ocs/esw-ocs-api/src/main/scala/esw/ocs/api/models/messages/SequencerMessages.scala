package esw.ocs.api.models.messages

import akka.Done
import akka.actor.typed.ActorRef
import csw.command.client.messages.sequencer.SequencerMsg
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.{Sequence, SequenceCommand}
import csw.params.core.models.Id
import esw.ocs.api.models.messages.SequencerResponses._
import esw.ocs.api.models.{SequencerState, Step, StepList}
import esw.ocs.api.serializer.OcsFrameworkAkkaSerializable

object SequencerMessages {

  sealed trait EswSequencerMessage extends SequencerMsg with OcsFrameworkAkkaSerializable

  sealed trait IdleMessage           extends EswSequencerMessage
  sealed trait SequenceLoadedMessage extends EswSequencerMessage
  sealed trait InProgressMessage     extends EswSequencerMessage
  sealed trait OfflineMessage        extends EswSequencerMessage

  final case class LoadSequence(sequence: Sequence, replyTo: ActorRef[LoadSequenceResponse]) extends IdleMessage
  final case class LoadAndProcess(sequence: Sequence, replyTo: ActorRef[SubmitResponse])     extends IdleMessage
  final case class StartSequence(replyTo: ActorRef[SubmitResponse])                          extends SequenceLoadedMessage

  // engine msgs
  final case class PullNext(replyTo: ActorRef[Step])              extends IdleMessage with InProgressMessage with SequenceLoadedMessage
  final case class MaybeNext(replyTo: ActorRef[Option[Step]])     extends InProgressMessage
  final case class ReadyToExecuteNext(replyTo: ActorRef[Done])    extends InProgressMessage
  final case class UpdateFailure(failureResponse: SubmitResponse) extends InProgressMessage

  // lifecycle msgs
  final case class GoOnline(replyTo: ActorRef[LifecycleResponse])  extends OfflineMessage
  final case class GoOffline(replyTo: ActorRef[LifecycleResponse]) extends IdleMessage with SequenceLoadedMessage
  final case class Shutdown(replyTo: ActorRef[LifecycleResponse])
      extends IdleMessage
      with SequenceLoadedMessage
      with InProgressMessage
      with OfflineMessage

  final case class Abort(replyTo: ActorRef[LifecycleResponse]) extends SequenceLoadedMessage with InProgressMessage

  // editor msgs
  // fixme : GetSequence and GetPreviousSequence should have replyTo StepListResponse
  final case class GetSequence(replyTo: ActorRef[StepList]) extends SequenceLoadedMessage with InProgressMessage

  final case class GetPreviousSequence(replyTo: ActorRef[StepListResponse])
      extends IdleMessage
      with SequenceLoadedMessage
      with InProgressMessage
      with OfflineMessage

  final case class Add(commands: List[SequenceCommand], replyTo: ActorRef[EditorResponse])
      extends SequenceLoadedMessage
      with InProgressMessage
  final case class Prepend(commands: List[SequenceCommand], replyTo: ActorRef[EditorResponse])
      extends SequenceLoadedMessage
      with InProgressMessage
  final case class Replace(id: Id, commands: List[SequenceCommand], replyTo: ActorRef[EditorResponse])
      extends SequenceLoadedMessage
      with InProgressMessage
  final case class InsertAfter(id: Id, commands: List[SequenceCommand], replyTo: ActorRef[EditorResponse])
      extends SequenceLoadedMessage
      with InProgressMessage
  final case class Delete(ids: Id, replyTo: ActorRef[EditorResponse])       extends SequenceLoadedMessage with InProgressMessage
  final case class AddBreakpoint(id: Id, replyTo: ActorRef[EditorResponse]) extends SequenceLoadedMessage with InProgressMessage
  final case class RemoveBreakpoint(id: Id, replyTo: ActorRef[EditorResponse])
      extends SequenceLoadedMessage
      with InProgressMessage
  final case class Pause(replyTo: ActorRef[EditorResponse])  extends SequenceLoadedMessage with InProgressMessage
  final case class Resume(replyTo: ActorRef[EditorResponse]) extends SequenceLoadedMessage with InProgressMessage
  final case class Reset(replyTo: ActorRef[EditorResponse])  extends SequenceLoadedMessage with InProgressMessage

  //internal
  final private[esw] case class UpdateSequencerState(state: SequencerState) extends InProgressMessage
  final private[esw] case class GoIdle(state: SequencerState)               extends InProgressMessage
}

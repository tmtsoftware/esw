package esw.ocs.api.models.messages.FSM

import akka.actor.typed.ActorRef
import csw.params.commands.Sequence

sealed trait SequencerMessage {
  val replyTo: ActorRef[Unhandled]
}
sealed trait IdleMessage           extends SequencerMessage
sealed trait InProgressMessage     extends SequencerMessage
sealed trait OfflineMessage        extends SequencerMessage
sealed trait SequenceLoadedMessage extends SequencerMessage

final case class Shutdown(replyTo: ActorRef[SequencerResponse])                         extends OfflineMessage with IdleMessage with SequenceLoadedMessage
final case class GoOnline(replyTo: ActorRef[SequencerResponse])                         extends OfflineMessage
final case class GoOffline(replyTo: ActorRef[SequencerResponse])                        extends IdleMessage with SequenceLoadedMessage
final case class LoadSequence(sequence: Sequence, replyTo: ActorRef[SequencerResponse]) extends IdleMessage

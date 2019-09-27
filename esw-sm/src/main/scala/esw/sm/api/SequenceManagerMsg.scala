package esw.sm.api

import akka.actor.typed.ActorRef
import csw.params.commands.Sequence
import csw.params.core.models.Id

sealed trait SequenceManagerMsg {
  val replyTo: ActorRef[_]
}

object SequenceManagerMsg {
  case class Shutdown(replyTo: ActorRef[Response]) extends SequenceManagerMsg

  case class AcceptSequence(sequence: Sequence, replyTo: ActorRef[Response])   extends SequenceManagerMsg
  case class ValidateSequence(sequence: Sequence, replyTo: ActorRef[Response]) extends SequenceManagerMsg
  case class StartSequence(runId: Id, replyTo: ActorRef[Response])             extends SequenceManagerMsg

  case class ListSequence(replyTo: ActorRef[List[RichSequence]])             extends SequenceManagerMsg
  case class GetSequence(runId: Id, replyTo: ActorRef[Option[RichSequence]]) extends SequenceManagerMsg

  case class StartSequencer(sequencerId: String, observingMode: String, replyTo: ActorRef[Response])    extends SequenceManagerMsg
  case class ShutdownSequencer(sequencerId: String, observingMode: String, replyTo: ActorRef[Response]) extends SequenceManagerMsg
  case class GoOnlineSequencer(sequencerId: String, observingMode: String, replyTo: ActorRef[Response]) extends SequenceManagerMsg
  case class GoOfflineSequencer(sequencerId: String, observingMode: String, replyTo: ActorRef[Response])
      extends SequenceManagerMsg
}

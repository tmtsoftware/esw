package esw.sm.api.actor.messages

import akka.actor.typed.ActorRef

sealed trait SequenceManagerMsg

object SequenceManagerMsg {
  case class Configure(obsMode: String, replyTo: ActorRef[ConfigureResponse])  extends SequenceManagerMsg
  case class Cleanup(obsMode: String, replyTo: ActorRef[CleanupResponse])      extends SequenceManagerMsg
  case class GetRunningObsModes(replyTo: ActorRef[GetRunningObsModesResponse]) extends SequenceManagerMsg

  private[sm] case class Configuring(res: ConfigureResponse) extends SequenceManagerMsg
  private[sm] case class CleaningUp(res: CleanupResponse)    extends SequenceManagerMsg
}

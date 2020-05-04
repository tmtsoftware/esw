package esw.sm.api.actor.messages

import akka.actor.typed.ActorRef
import esw.sm.api.SequenceManagerState

sealed trait SequenceManagerMsg

object SequenceManagerMsg {
  case class Configure(obsMode: String, replyTo: ActorRef[ConfigureResponse])  extends SequenceManagerMsg
  case class Cleanup(obsMode: String, replyTo: ActorRef[CleanupResponse])      extends SequenceManagerMsg
  case class GetRunningObsModes(replyTo: ActorRef[GetRunningObsModesResponse]) extends SequenceManagerMsg
  case class GetSequenceManagerState(replyTo: ActorRef[SequenceManagerState])  extends SequenceManagerMsg

  private[sm] case class ConfigurationDone(res: ConfigureResponse) extends SequenceManagerMsg
  private[sm] case class CleanupDone(res: CleanupResponse)         extends SequenceManagerMsg
}

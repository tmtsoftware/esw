package esw.sm.api.actor.messages

import akka.actor.typed.ActorRef
import esw.sm.api.models.{CleanupResponse, ConfigureResponse, GetRunningObsModesResponse}

sealed trait SequenceManagerMsg

object SequenceManagerMsg {
  case class Configure(obsMode: String, replyTo: ActorRef[ConfigureResponse])  extends SequenceManagerMsg
  case class Cleanup(obsMode: String, replyTo: ActorRef[CleanupResponse])      extends SequenceManagerMsg
  sealed trait CommonMessage                                                   extends SequenceManagerMsg
  case class GetRunningObsModes(replyTo: ActorRef[GetRunningObsModesResponse]) extends CommonMessage

  private[sm] case class ConfigurationDone(res: ConfigureResponse) extends SequenceManagerMsg
  private[sm] case class CleanupDone(res: CleanupResponse)         extends SequenceManagerMsg
}

package esw.sm.api.actor.messages

import akka.actor.typed.ActorRef
import esw.sm.api.SequenceManagerState
import esw.sm.api.models.{CleanupResponse, ConfigureResponse, GetRunningObsModesResponse}

sealed trait SequenceManagerMsg

object SequenceManagerMsg {
  case class Configure(obsMode: String, replyTo: ActorRef[ConfigureResponse])  extends SequenceManagerMsg
  case class Cleanup(obsMode: String, replyTo: ActorRef[CleanupResponse])      extends SequenceManagerMsg
  sealed trait CommonMessage                                                   extends SequenceManagerMsg
  case class GetRunningObsModes(replyTo: ActorRef[GetRunningObsModesResponse]) extends CommonMessage
  case class GetSequenceManagerState(replyTo: ActorRef[SequenceManagerState])  extends CommonMessage

  private[sm] case class ConfigurationInternal(res: ConfigureResponse) extends SequenceManagerMsg
  private[sm] case class CleanupInternal(res: CleanupResponse)         extends SequenceManagerMsg
}

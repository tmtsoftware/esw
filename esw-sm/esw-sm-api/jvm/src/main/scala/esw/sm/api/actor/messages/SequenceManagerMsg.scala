package esw.sm.api.actor.messages

import akka.actor.typed.ActorRef
import esw.sm.api.SequenceManagerState
import esw.sm.api.codecs.SmAkkaSerializable
import esw.sm.api.models.{CleanupResponse, ConfigureResponse, GetRunningObsModesResponse}

sealed trait SequenceManagerMsg

sealed trait SequenceManagerRemoteMsg extends SequenceManagerMsg with SmAkkaSerializable

object SequenceManagerMsg {
  case class Configure(obsMode: String, replyTo: ActorRef[ConfigureResponse])  extends SequenceManagerRemoteMsg
  case class Cleanup(obsMode: String, replyTo: ActorRef[CleanupResponse])      extends SequenceManagerRemoteMsg
  sealed trait CommonMessage                                                   extends SequenceManagerRemoteMsg
  case class GetRunningObsModes(replyTo: ActorRef[GetRunningObsModesResponse]) extends CommonMessage
  case class GetSequenceManagerState(replyTo: ActorRef[SequenceManagerState])  extends CommonMessage

  private[sm] case class ConfigurationResponseInternal(res: ConfigureResponse) extends SequenceManagerMsg
  private[sm] case class CleanupResponseInternal(res: CleanupResponse)         extends SequenceManagerMsg
}

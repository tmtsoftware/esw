package esw.sm.api.actor.messages

import akka.actor.typed.ActorRef
import csw.prefix.models.Subsystem
import esw.sm.api.SequenceManagerState
import esw.sm.api.codecs.SmAkkaSerializable
import esw.sm.api.models._

sealed trait SequenceManagerMsg

sealed trait SequenceManagerRemoteMsg   extends SequenceManagerMsg with SmAkkaSerializable
sealed trait SequenceManagerInternalMsg extends SequenceManagerMsg

sealed trait SequenceManagerIdleMsg extends SequenceManagerRemoteMsg

object SequenceManagerMsg {
  case class Configure(obsMode: String, replyTo: ActorRef[ConfigureResponse]) extends SequenceManagerIdleMsg
  case class Cleanup(obsMode: String, replyTo: ActorRef[CleanupResponse])     extends SequenceManagerIdleMsg
  case class StartSequencer(subsystem: Subsystem, observingMode: String, replyTo: ActorRef[StartSequencerResponse])
      extends SequenceManagerIdleMsg
  case class ShutdownSequencer(subsystem: Subsystem, observingMode: String, replyTo: ActorRef[ShutdownSequencerResponse])
      extends SequenceManagerIdleMsg
  case class RestartSequencer(subsystem: Subsystem, observingMode: String, replyTo: ActorRef[RestartSequencerResponse])
      extends SequenceManagerIdleMsg
  case class ShutdownAllSequencers(replyTo: ActorRef[ShutdownAllSequencersResponse]) extends SequenceManagerIdleMsg

  sealed trait CommonMessage                                                   extends SequenceManagerRemoteMsg
  case class GetRunningObsModes(replyTo: ActorRef[GetRunningObsModesResponse]) extends CommonMessage
  case class GetSequenceManagerState(replyTo: ActorRef[SequenceManagerState])  extends CommonMessage

  private[sm] case class StartSequencerResponseInternal(res: StartSequencerResponse)       extends SequenceManagerInternalMsg
  private[sm] case class ShutdownSequencerResponseInternal(res: ShutdownSequencerResponse) extends SequenceManagerInternalMsg
  private[sm] case class RestartSequencerResponseInternal(res: RestartSequencerResponse)   extends SequenceManagerInternalMsg
  private[sm] case class ConfigurationResponseInternal(res: ConfigureResponse)             extends SequenceManagerInternalMsg
  private[sm] case class CleanupResponseInternal(res: CleanupResponse)                     extends SequenceManagerInternalMsg
  private[sm] case class ShutdownAllSequencersResponseInternal(res: ShutdownAllSequencersResponse)
      extends SequenceManagerInternalMsg
}

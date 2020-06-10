package esw.sm.api.actor.messages

import akka.actor.typed.ActorRef
import csw.prefix.models.Subsystem
import esw.sm.api.SequenceManagerState
import esw.sm.api.codecs.SmAkkaSerializable
import esw.sm.api.models._

sealed trait SequenceManagerMsg

sealed trait SequenceManagerRemoteMsg    extends SequenceManagerMsg with SmAkkaSerializable
sealed trait SequencerManagerInternalMsg extends SequenceManagerMsg

object SequenceManagerMsg {
  case class Configure(obsMode: String, replyTo: ActorRef[ConfigureResponse])  extends SequenceManagerRemoteMsg
  case class Cleanup(obsMode: String, replyTo: ActorRef[CleanupResponse])      extends SequenceManagerRemoteMsg
  sealed trait CommonMessage                                                   extends SequenceManagerRemoteMsg
  case class GetRunningObsModes(replyTo: ActorRef[GetRunningObsModesResponse]) extends CommonMessage
  case class GetSequenceManagerState(replyTo: ActorRef[SequenceManagerState])  extends CommonMessage
  case class StartSequencer(subsystem: Subsystem, observingMode: String, replyTo: ActorRef[StartSequencerResponse])
      extends SequenceManagerRemoteMsg
  case class ShutdownSequencer(subsystem: Subsystem, observingMode: String, replyTo: ActorRef[ShutdownSequencerResponse])
      extends SequenceManagerRemoteMsg
  case class RestartSequencer(subsystem: Subsystem, observingMode: String, replyTo: ActorRef[RestartSequencerResponse])
      extends SequenceManagerRemoteMsg
  case class ShutdownAllSequencers(replyTo: ActorRef[ShutdownAllSequencersResponse]) extends SequenceManagerRemoteMsg

  private[sm] case class StartSequencerResponseInternal(res: StartSequencerResponse)       extends SequencerManagerInternalMsg
  private[sm] case class ShutdownSequencerResponseInternal(res: ShutdownSequencerResponse) extends SequencerManagerInternalMsg
  private[sm] case class RestartSequencerResponseInternal(res: RestartSequencerResponse)   extends SequencerManagerInternalMsg
  private[sm] case class ConfigurationResponseInternal(res: ConfigureResponse)             extends SequencerManagerInternalMsg
  private[sm] case class CleanupResponseInternal(res: CleanupResponse)                     extends SequencerManagerInternalMsg
  private[sm] case class ShutdownAllSequencersResponseInternal(res: ShutdownAllSequencersResponse)
      extends SequencerManagerInternalMsg
}

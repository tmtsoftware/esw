package esw.sm.api.actor.messages

import akka.actor.typed.ActorRef
import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.models.ObsMode
import esw.sm.api.SequenceManagerState
import esw.sm.api.codecs.SmAkkaSerializable
import esw.sm.api.protocol._

sealed trait SequenceManagerMsg

sealed trait SequenceManagerRemoteMsg   extends SequenceManagerMsg with SmAkkaSerializable
sealed trait SequenceManagerInternalMsg extends SequenceManagerMsg

sealed trait SequenceManagerIdleMsg extends SequenceManagerRemoteMsg

object SequenceManagerMsg {
  case class Configure(obsMode: ObsMode, replyTo: ActorRef[ConfigureResponse]) extends SequenceManagerIdleMsg
  case class Cleanup(obsMode: ObsMode, replyTo: ActorRef[CleanupResponse])     extends SequenceManagerIdleMsg
  case class StartSequencer(subsystem: Subsystem, observingMode: ObsMode, replyTo: ActorRef[StartSequencerResponse])
      extends SequenceManagerIdleMsg
  case class ShutdownSequencer(
      subsystem: Subsystem,
      observingMode: ObsMode,
      replyTo: ActorRef[ShutdownSequencerResponse]
  ) extends SequenceManagerIdleMsg
  case class RestartSequencer(subsystem: Subsystem, observingMode: ObsMode, replyTo: ActorRef[RestartSequencerResponse])
      extends SequenceManagerIdleMsg
  case class ShutdownAllSequencers(replyTo: ActorRef[ShutdownAllSequencersResponse]) extends SequenceManagerIdleMsg
  case class SpawnSequenceComponent(
      agent: Prefix,
      sequenceComponentName: String,
      replyTo: ActorRef[SpawnSequenceComponentResponse]
  ) extends SequenceManagerIdleMsg
  case class ShutdownSequenceComponent(prefix: Prefix, replyTo: ActorRef[ShutdownSequenceComponentResponse])
      extends SequenceManagerIdleMsg

  sealed trait CommonMessage                                                   extends SequenceManagerRemoteMsg
  case class GetRunningObsModes(replyTo: ActorRef[GetRunningObsModesResponse]) extends CommonMessage
  case class GetSequenceManagerState(replyTo: ActorRef[SequenceManagerState])  extends CommonMessage

  private[sm] case class ConfigurationResponseInternal(res: ConfigureResponse) extends SequenceManagerInternalMsg
  private[sm] case class CleanupResponseInternal(res: CleanupResponse)         extends SequenceManagerInternalMsg
}

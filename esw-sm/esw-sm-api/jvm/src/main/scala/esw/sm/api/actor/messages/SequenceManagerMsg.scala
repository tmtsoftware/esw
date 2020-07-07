package esw.sm.api.actor.messages

import akka.actor.typed.ActorRef
import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.models.ObsMode
import esw.sm.api.SequenceManagerState
import esw.sm.api.codecs.SmAkkaSerializable
import esw.sm.api.protocol._

sealed trait SequenceManagerMsg

sealed trait SequenceManagerRemoteMsg extends SequenceManagerMsg with SmAkkaSerializable

sealed trait SequenceManagerIdleMsg extends SequenceManagerRemoteMsg

object SequenceManagerMsg {
  case class Configure(obsMode: ObsMode, replyTo: ActorRef[ConfigureResponse]) extends SequenceManagerIdleMsg

  case class StartSequencer(subsystem: Subsystem, obsMode: ObsMode, replyTo: ActorRef[StartSequencerResponse])
      extends SequenceManagerIdleMsg
  case class RestartSequencer(subsystem: Subsystem, obsMode: ObsMode, replyTo: ActorRef[RestartSequencerResponse])
      extends SequenceManagerIdleMsg

  case class ShutdownSequencers(policy: ShutdownSequencersPolicy, replyTo: ActorRef[ShutdownSequencersResponse])
      extends SequenceManagerIdleMsg

  case class SpawnSequenceComponent(
      machine: Prefix,
      sequenceComponentName: String,
      replyTo: ActorRef[SpawnSequenceComponentResponse]
  ) extends SequenceManagerIdleMsg

  case class ShutdownSequenceComponents(
      policy: ShutdownSequenceComponentPolicy,
      replyTo: ActorRef[ShutdownSequenceComponentResponse]
  ) extends SequenceManagerIdleMsg

  sealed trait CommonMessage                                                   extends SequenceManagerRemoteMsg
  case class GetRunningObsModes(replyTo: ActorRef[GetRunningObsModesResponse]) extends CommonMessage
  case class GetSequenceManagerState(replyTo: ActorRef[SequenceManagerState])  extends CommonMessage

  private[sm] case class ProcessingComplete[T <: SmResponse](res: T) extends SequenceManagerMsg
}

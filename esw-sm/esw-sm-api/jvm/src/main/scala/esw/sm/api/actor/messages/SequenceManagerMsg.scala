package esw.sm.api.actor.messages

import akka.actor.typed.ActorRef
import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.models.ObsMode
import esw.sm.api.codecs.SmAkkaSerializable
import esw.sm.api.models.{ProvisionConfig, SequenceManagerState}
import esw.sm.api.protocol.*

// Messages for the sequence manager actor
sealed trait SequenceManagerMsg

sealed trait SequenceManagerRemoteMsg extends SequenceManagerMsg with SmAkkaSerializable

sealed trait UnhandleableSequenceManagerMsg extends SequenceManagerRemoteMsg {
  def replyTo: ActorRef[Unhandled]
}

sealed trait SequenceManagerIdleMsg extends SequenceManagerRemoteMsg with UnhandleableSequenceManagerMsg
sealed trait CommonMessage          extends SequenceManagerRemoteMsg

object SequenceManagerMsg {
  case class Configure(obsMode: ObsMode, replyTo: ActorRef[ConfigureResponse]) extends SequenceManagerIdleMsg

  case class StartSequencer(prefix: Prefix, replyTo: ActorRef[StartSequencerResponse])     extends SequenceManagerIdleMsg
  case class RestartSequencer(prefix: Prefix, replyTo: ActorRef[RestartSequencerResponse]) extends SequenceManagerIdleMsg

  case class ShutdownSequencer(prefix: Prefix, replyTo: ActorRef[ShutdownSequencersResponse]) extends SequenceManagerIdleMsg
  case class ShutdownSubsystemSequencers(subsystem: Subsystem, replyTo: ActorRef[ShutdownSequencersResponse])
      extends SequenceManagerIdleMsg
  case class ShutdownObsModeSequencers(obsMode: ObsMode, replyTo: ActorRef[ShutdownSequencersResponse])
      extends SequenceManagerIdleMsg
  case class ShutdownAllSequencers(replyTo: ActorRef[ShutdownSequencersResponse]) extends SequenceManagerIdleMsg

  case class Provision(config: ProvisionConfig, replyTo: ActorRef[ProvisionResponse]) extends SequenceManagerIdleMsg

  case class ShutdownSequenceComponent(prefix: Prefix, replyTo: ActorRef[ShutdownSequenceComponentResponse])
      extends SequenceManagerIdleMsg
  case class ShutdownAllSequenceComponents(replyTo: ActorRef[ShutdownSequenceComponentResponse]) extends SequenceManagerIdleMsg

  case class GetObsModesDetails(replyTo: ActorRef[ObsModesDetailsResponse])   extends CommonMessage
  case class GetSequenceManagerState(replyTo: ActorRef[SequenceManagerState]) extends CommonMessage
  case class GetResources(replyTo: ActorRef[ResourcesStatusResponse])         extends CommonMessage

  private[sm] case class ProcessingComplete[T <: SmResponse](res: T) extends SequenceManagerMsg
}

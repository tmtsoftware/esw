package esw.sm.api

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import csw.commons.CborAkkaSerializer
import esw.sm.api.actor.codecs.SequenceManagerMsgCodecs
import esw.sm.api.actor.messages.SequenceManagerRemoteMsg
import esw.sm.api.codecs.SmAkkaSerializable
import esw.sm.api.models.SequenceManagerState
import esw.sm.api.protocol._

class SmAkkaSerializer(_actorSystem: ExtendedActorSystem)
    extends CborAkkaSerializer[SmAkkaSerializable]
    with SequenceManagerMsgCodecs {
  implicit def actorSystem: ActorSystem[_] = _actorSystem.toTyped

  override def identifier: Int = 29945

  register[SequenceManagerRemoteMsg]
  register[StartSequencerResponse]
  register[ShutdownSequenceComponentResponse]
  register[RestartSequencerResponse]
  register[ShutdownSequencersResponse]
  register[GetRunningObsModesResponse]
  register[ObsModesWithStatusResponse]
  register[ConfigureResponse]
  register[AgentStatusResponse]
  register[SequenceManagerState]
  register[ProvisionResponse]
  register[ResourcesStatusResponse]
}

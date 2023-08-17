package esw.sm.api

import org.apache.pekko.actor.ExtendedActorSystem
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.adapter.*
import csw.commons.CborPekkoSerializer
import esw.sm.api.actor.codecs.SequenceManagerMsgCodecs
import esw.sm.api.actor.messages.SequenceManagerRemoteMsg
import esw.sm.api.codecs.SmPekkoSerializable
import esw.sm.api.models.SequenceManagerState
import esw.sm.api.protocol.*

//Serializer for the Sequence Manager actor
class SmPekkoSerializer(_actorSystem: ExtendedActorSystem)
    extends CborPekkoSerializer[SmPekkoSerializable]
    with SequenceManagerMsgCodecs {
  implicit def actorSystem: ActorSystem[_] = _actorSystem.toTyped

  override def identifier: Int = 29945

  register[SequenceManagerRemoteMsg]
  register[StartSequencerResponse]
  register[ShutdownSequenceComponentResponse]
  register[RestartSequencerResponse]
  register[ShutdownSequencersResponse]
  register[ObsModesDetailsResponse]
  register[ConfigureResponse]
  register[SequenceManagerState]
  register[ProvisionResponse]
  register[ResourcesStatusResponse]
  register[FailedResponse]
}

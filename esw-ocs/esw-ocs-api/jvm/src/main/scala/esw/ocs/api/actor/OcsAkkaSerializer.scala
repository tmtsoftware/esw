package esw.ocs.api.actor

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.serialization.Serializer
import csw.command.client.messages.sequencer.SequencerMsg
import csw.commons.cbor.CborAkkaSerializer
import esw.ocs.api.actor.messages.SequencerMessages._
import esw.ocs.api.actor.messages.{SequenceComponentMsg, SequencerState}
import esw.ocs.api.codecs.{OcsAkkaSerializable, OcsCodecs}
import esw.ocs.api.models.StepList
import esw.ocs.api.protocol.{EswSequencerResponse, GetStatusResponse, ScriptResponse}

class OcsAkkaSerializer(_actorSystem: ExtendedActorSystem)
    extends CborAkkaSerializer[OcsAkkaSerializable]
    with OcsCodecs
    with OcsMsgCodecs
    with Serializer {
  override implicit def actorSystem: ActorSystem[_] = _actorSystem.toTyped

  override def identifier: Int = 29926

  register[EswSequencerRemoteMessage]
  register[EswSequencerResponse]
  register[StepList]
  register[SequencerState[SequencerMsg]]
  register[SequenceComponentMsg]
  register[ScriptResponse]
  register[GetStatusResponse]
}

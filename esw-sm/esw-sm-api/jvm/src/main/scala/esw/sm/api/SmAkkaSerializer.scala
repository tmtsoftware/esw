package esw.sm.api

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import csw.commons.CborAkkaSerializer
import esw.sm.api.actor.codecs.SequenceManagerMsgCodecs
import esw.sm.api.actor.messages.SequenceManagerRemoteMsg
import esw.sm.api.codecs.SmAkkaSerializable
import esw.sm.api.models.{CleanupResponse, ConfigureResponse, GetRunningObsModesResponse}

class SmAkkaSerializer(_actorSystem: ExtendedActorSystem)
    extends CborAkkaSerializer[SmAkkaSerializable]
    with SequenceManagerMsgCodecs {
  implicit def actorSystem: ActorSystem[_] = _actorSystem.toTyped

  override def identifier: Int = 29945

  register[SequenceManagerRemoteMsg]
  register[CleanupResponse]
  register[GetRunningObsModesResponse]
  register[ConfigureResponse]
  register[SequenceManagerState]
}

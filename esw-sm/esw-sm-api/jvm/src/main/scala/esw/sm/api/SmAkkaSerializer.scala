/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.sm.api

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.*
import csw.commons.CborAkkaSerializer
import esw.sm.api.actor.codecs.SequenceManagerMsgCodecs
import esw.sm.api.actor.messages.SequenceManagerRemoteMsg
import esw.sm.api.codecs.SmAkkaSerializable
import esw.sm.api.models.SequenceManagerState
import esw.sm.api.protocol.*

//Serializer for the Sequence Manager actor
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
  register[ObsModesDetailsResponse]
  register[ConfigureResponse]
  register[SequenceManagerState]
  register[ProvisionResponse]
  register[ResourcesStatusResponse]
  register[FailedResponse]
}

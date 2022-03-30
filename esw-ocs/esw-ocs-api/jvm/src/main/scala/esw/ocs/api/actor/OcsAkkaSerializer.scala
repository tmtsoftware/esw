/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.api.actor

import csw.command.client.messages.sequencer.SequencerMsg
import csw.commons.CborAkkaSerializer
import esw.ocs.api.actor.messages.SequencerMessages.*
import esw.ocs.api.actor.messages.{InternalSequencerState, SequenceComponentRemoteMsg}
import esw.ocs.api.codecs.{OcsAkkaSerializable, OcsCodecs}
import esw.ocs.api.models.StepList
import esw.ocs.api.protocol.EswSequencerResponse
import esw.ocs.api.protocol.SequenceComponentResponse.{GetStatusResponse, OkOrUnhandled, ScriptResponseOrUnhandled}

/*
 * Serializer being used in ser(de) of sequencer's/sequencerComponent's actor messages
 */
class OcsAkkaSerializer extends CborAkkaSerializer[OcsAkkaSerializable] with OcsCodecs with OcsMsgCodecs {

  override def identifier: Int = 29926

  register[EswSequencerRemoteMessage]
  register[EswSequencerResponse]
  register[StepList]
  register[InternalSequencerState[SequencerMsg]]
  register[SequenceComponentRemoteMsg]
  register[ScriptResponseOrUnhandled]
  register[GetStatusResponse]
  register[OkOrUnhandled]
}

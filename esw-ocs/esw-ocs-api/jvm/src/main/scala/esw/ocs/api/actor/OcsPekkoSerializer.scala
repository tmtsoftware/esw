package esw.ocs.api.actor

import csw.command.client.messages.sequencer.SequencerMsg
import csw.commons.CborPekkoSerializer
import esw.ocs.api.actor.messages.SequencerMessages.*
import esw.ocs.api.actor.messages.{InternalSequencerState, SequenceComponentRemoteMsg}
import esw.ocs.api.codecs.{OcsPekkoSerializable, OcsCodecs}
import esw.ocs.api.models.StepList
import esw.ocs.api.protocol.EswSequencerResponse
import esw.ocs.api.protocol.SequenceComponentResponse.{GetStatusResponse, OkOrUnhandled, ScriptResponseOrUnhandled}

/*
 * Serializer being used in ser(de) of sequencer's/sequencerComponent's actor messages
 */
class OcsPekkoSerializer extends CborPekkoSerializer[OcsPekkoSerializable] with OcsCodecs with OcsMsgCodecs {

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

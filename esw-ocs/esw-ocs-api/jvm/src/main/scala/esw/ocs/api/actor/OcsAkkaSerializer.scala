package esw.ocs.api.actor

import csw.command.client.messages.sequencer.SequencerMsg
import csw.commons.CborAkkaSerializer
import esw.ocs.api.actor.messages.SequencerMessages._
import esw.ocs.api.actor.messages.{SequenceComponentRemoteMsg, SequencerState}
import esw.ocs.api.codecs.{OcsAkkaSerializable, OcsCodecs}
import esw.ocs.api.models.StepList
import esw.ocs.api.protocol.EswSequencerResponse
import esw.ocs.api.protocol.SequenceComponentResponse.{GetStatusResponse, OkOrUnhandled, ScriptResponseOrUnhandled}

class OcsAkkaSerializer extends CborAkkaSerializer[OcsAkkaSerializable] with OcsCodecs with OcsMsgCodecs {

  override def identifier: Int = 29926

  register[EswSequencerRemoteMessage]
  register[EswSequencerResponse]
  register[StepList]
  register[SequencerState[SequencerMsg]]
  register[SequenceComponentRemoteMsg]
  register[ScriptResponseOrUnhandled]
  register[GetStatusResponse]
  register[OkOrUnhandled]
}

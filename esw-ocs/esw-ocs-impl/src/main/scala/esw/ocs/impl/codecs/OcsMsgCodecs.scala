package esw.ocs.impl.codecs

import csw.command.client.cbor.MessageCodecs
import csw.command.client.messages.sequencer.SequencerMsg
import csw.prefix.codecs.CommonCodecs
import esw.ocs.api.actor.messages.SequencerMessages._
import esw.ocs.api.actor.messages.{SequenceComponentMsg, SequencerState}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs

trait OcsMsgCodecs extends MessageCodecs with CommonCodecs {
  implicit lazy val eswSequencerMessageCodec: Codec[EswSequencerRemoteMessage]       = deriveAllCodecs
  implicit lazy val sequenceComponentMsgCodec: Codec[SequenceComponentMsg]           = deriveAllCodecs
  implicit lazy val sequencerBehaviorStateCodec: Codec[SequencerState[SequencerMsg]] = enumCodec[SequencerState[SequencerMsg]]
}

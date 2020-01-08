package esw.ocs.impl.codecs

import csw.command.client.cbor.MessageCodecs
import csw.command.client.messages.sequencer.SequencerMsg
import csw.prefix.codecs.CommonCodecs
import esw.ocs.impl.messages.SequencerMessages._
import esw.ocs.impl.messages.{SequenceComponentMsg, SequencerState}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.CompactMapBasedCodecs._

trait OcsMsgCodecs extends MessageCodecs with CommonCodecs {
  implicit lazy val loadSequenceCodec: Codec[LoadSequence]                           = deriveCodec
  implicit lazy val goOfflineCodec: Codec[GoOffline]                                 = deriveCodec
  implicit lazy val pullNextCodec: Codec[PullNext]                                   = deriveCodec
  implicit lazy val insertAfterCodec: Codec[EditorAction]                            = deriveAllCodecs
  implicit lazy val eswSequencerMessageCodec: Codec[EswSequencerMessage]             = deriveAllCodecs
  implicit lazy val sequencerBehaviorStateCodec: Codec[SequencerState[SequencerMsg]] = enumCodec[SequencerState[SequencerMsg]]
  implicit lazy val sequenceComponentMsgCodec: Codec[SequenceComponentMsg]           = deriveAllCodecs
}

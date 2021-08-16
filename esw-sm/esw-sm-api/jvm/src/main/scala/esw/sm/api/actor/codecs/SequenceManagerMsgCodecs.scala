package esw.sm.api.actor.codecs

import csw.command.client.cbor.MessageCodecs
import csw.prefix.codecs.CommonCodecs
import esw.sm.api.actor.messages.SequenceManagerRemoteMsg
import esw.sm.api.codecs.SequenceManagerCodecs
import esw.sm.api.models.SequenceManagerState
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs

//Codecs for ser(de) for the SequenceManager messages
trait SequenceManagerMsgCodecs extends SequenceManagerCodecs with MessageCodecs with CommonCodecs {
  implicit lazy val sequenceManagerRemoteMsgCodec: Codec[SequenceManagerRemoteMsg] = deriveAllCodecs
  implicit lazy val sequenceManagerStateCodec: Codec[SequenceManagerState]         = enumCodec[SequenceManagerState]
}

package esw.ocs.framework.api.models.codecs

import csw.command.client.cbor.MessageCodecs
import csw.location.api.codec.DoneCodec
import csw.location.model.codecs.LocationCodecs
import esw.ocs.framework.api.models.messages.SequenceComponentMsg
import esw.ocs.framework.api.models.messages.SequenceComponentMsg.{GetStatus, LoadScript, UnloadScript}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec

trait SequenceComponentCodecs extends LocationCodecs with DoneCodec with MessageCodecs {
  implicit lazy val loadScriptCodec: Codec[LoadScript]                     = deriveCodec[LoadScript]
  implicit lazy val getStatusCodec: Codec[GetStatus]                       = deriveCodec[GetStatus]
  implicit lazy val unloadScriptCodec: Codec[UnloadScript]                 = deriveCodec[UnloadScript]
  implicit lazy val sequenceComponentMsgCodec: Codec[SequenceComponentMsg] = deriveCodec[SequenceComponentMsg]

  //fixme:  check if it works without DoneCodecs and LocationCodecs and ActorRefCodec
}

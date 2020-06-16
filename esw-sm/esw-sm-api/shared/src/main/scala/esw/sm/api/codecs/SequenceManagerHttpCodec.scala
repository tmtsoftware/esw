package esw.sm.api.codecs

import esw.sm.api.protocol.{SequenceManagerPostRequest, SequenceManagerWebsocketRequest}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs
import msocket.api
import msocket.api.ErrorProtocol
import msocket.api.models.ServiceError

object SequenceManagerHttpCodec extends SequenceManagerHttpCodec

trait SequenceManagerHttpCodec extends SequenceManagerCodecs {

  implicit val smPostRequestCodec: Codec[SequenceManagerPostRequest]           = deriveAllCodecs
  implicit val smWebsocketRequestCodec: Codec[SequenceManagerWebsocketRequest] = deriveAllCodecs

  implicit lazy val smPostRequestErrorProtocol: ErrorProtocol[SequenceManagerPostRequest] =
    ErrorProtocol.bind[SequenceManagerPostRequest, ServiceError]

  implicit lazy val smWebsocketErrorProtocol: api.ErrorProtocol[SequenceManagerWebsocketRequest] =
    ErrorProtocol.bind[SequenceManagerWebsocketRequest, ServiceError]
}

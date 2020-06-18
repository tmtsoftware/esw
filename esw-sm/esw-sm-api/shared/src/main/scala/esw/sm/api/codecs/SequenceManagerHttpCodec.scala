package esw.sm.api.codecs

import esw.sm.api.protocol.SequenceManagerPostRequest
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs
import msocket.api.ErrorProtocol
import msocket.api.models.ServiceError

object SequenceManagerHttpCodec extends SequenceManagerHttpCodec

trait SequenceManagerHttpCodec extends SequenceManagerCodecs {

  implicit val smPostRequestCodec: Codec[SequenceManagerPostRequest] = deriveAllCodecs

  implicit lazy val smPostRequestErrorProtocol: ErrorProtocol[SequenceManagerPostRequest] =
    ErrorProtocol.bind[SequenceManagerPostRequest, ServiceError]
}

package esw.sm.api.codecs

import esw.sm.api.protocol.SequenceManagerRequest
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs
import msocket.api.ErrorProtocol
import msocket.api.models.ServiceError

object SequenceManagerServiceCodecs extends SequenceManagerServiceCodecs

trait SequenceManagerServiceCodecs extends SequenceManagerCodecs {

  implicit val smPostRequestCodec: Codec[SequenceManagerRequest] = deriveAllCodecs

  implicit lazy val smPostRequestErrorProtocol: ErrorProtocol[SequenceManagerRequest] =
    ErrorProtocol.bind[SequenceManagerRequest, ServiceError]
}

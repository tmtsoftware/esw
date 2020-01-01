package esw.ocs.api.codecs

import esw.ocs.api.protocol.{SequencerPostRequest, SequencerWebsocketRequest}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.CompactMapBasedCodecs._
import msocket.api
import msocket.api.ErrorProtocol
import msocket.api.models.ServiceError

object SequencerHttpCodecs extends SequencerHttpCodecs

trait SequencerHttpCodecs extends OcsCodecs {
  implicit lazy val sequencerPostRequestValue: Codec[SequencerPostRequest]           = deriveAllCodecs
  implicit lazy val sequencerWebsocketRequestValue: Codec[SequencerWebsocketRequest] = deriveAllCodecs

  implicit lazy val sequencerPostRequestErrorProtocol: ErrorProtocol[SequencerPostRequest] =
    ErrorProtocol.bind[SequencerPostRequest, ServiceError]

  implicit lazy val sequencerWebsocketErrorProtocol: api.ErrorProtocol[SequencerWebsocketRequest] =
    ErrorProtocol.bind[SequencerWebsocketRequest, ServiceError]
}

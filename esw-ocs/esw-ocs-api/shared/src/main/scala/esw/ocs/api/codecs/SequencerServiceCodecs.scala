package esw.ocs.api.codecs

import esw.ocs.api.protocol.{SequencerRequest, SequencerStreamRequest}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs
import msocket.api
import msocket.api.ErrorProtocol
import msocket.api.models.ServiceError

/**
 * Codecs for request(http/websocket) models of the sequencer
 */
object SequencerServiceCodecs extends SequencerServiceCodecs

trait SequencerServiceCodecs extends OcsCodecs {
  implicit lazy val sequencerPostRequestValue: Codec[SequencerRequest]            = deriveAllCodecs
  implicit lazy val sequencerWebsocketRequestValue: Codec[SequencerStreamRequest] = deriveAllCodecs

  implicit lazy val sequencerPostRequestErrorProtocol: ErrorProtocol[SequencerRequest] =
    ErrorProtocol.bind[SequencerRequest, ServiceError]

  implicit lazy val sequencerWebsocketErrorProtocol: api.ErrorProtocol[SequencerStreamRequest] =
    ErrorProtocol.bind[SequencerStreamRequest, ServiceError]
}

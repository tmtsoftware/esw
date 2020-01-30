package esw.gateway.api.codecs

import com.github.ghik.silencer.silent
import csw.alarm.models.Key.AlarmKey
import csw.command.api.codecs.CommandServiceCodecs
import csw.location.api.codec.LocationCodecs
import csw.logging.models.codecs.LoggingCodecs
import csw.params.events.EventKey
import esw.gateway.api.protocol._
import esw.ocs.api.codecs.SequencerHttpCodecs
import io.bullet.borer.Dom.MapElem
import io.bullet.borer.derivation.CompactMapBasedCodecs.deriveCodec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs
import io.bullet.borer.{Codec, Decoder, Encoder}
import msocket.api.ErrorProtocol

object GatewayCodecs extends GatewayCodecs
trait GatewayCodecs extends CommandServiceCodecs with LocationCodecs with LoggingCodecs with SequencerHttpCodecs {

  implicit lazy val gatewayExceptionCodecValue: Codec[GatewayException] = deriveAllCodecs

  implicit lazy val postRequestValue: Codec[PostRequest] = {
    @silent implicit lazy val metadataEnc: Encoder[Map[String, Any]] = Encoder[MapElem].contramap(ElementConverter.fromMap)
    @silent implicit lazy val metadataDec: Decoder[Map[String, Any]] = Decoder[MapElem].map(ElementConverter.toMap)
    deriveAllCodecs
  }

  implicit lazy val websocketRequestCodecValue: Codec[WebsocketRequest] = deriveAllCodecs

  //Todo: move to csw
  implicit lazy val eventKeyCodec: Codec[EventKey] = deriveCodec
  implicit lazy val alarmKeyCodec: Codec[AlarmKey] = deriveCodec

  implicit lazy val PostRequestErrorProtocol: ErrorProtocol[PostRequest] = ErrorProtocol.bind[PostRequest, GatewayException]
  implicit lazy val WebsocketRequestErrorProtocol: ErrorProtocol[WebsocketRequest] =
    ErrorProtocol.bind[WebsocketRequest, GatewayException]
}

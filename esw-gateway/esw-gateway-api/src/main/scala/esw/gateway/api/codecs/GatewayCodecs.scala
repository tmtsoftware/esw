package esw.gateway.api.codecs

import com.github.ghik.silencer.silent
import csw.alarm.models.Key.AlarmKey
import csw.command.api.codecs.CommandServiceCodecs
import csw.location.models.codecs.LocationCodecs
import csw.logging.models.codecs.LoggingCodecs
import csw.params.events.EventKey
import esw.gateway.api.protocol._
import esw.ocs.api.codecs.SequencerHttpCodecs
import io.bullet.borer.Dom.MapElem
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec
import io.bullet.borer.derivation.ArrayBasedCodecs.deriveUnaryCodec
import io.bullet.borer.{Codec, Decoder, Encoder}
import msocket.api.ErrorProtocol

object GatewayCodecs extends GatewayCodecs
trait GatewayCodecs extends CommandServiceCodecs with LocationCodecs with LoggingCodecs with SequencerHttpCodecs {

  implicit def gatewayExceptionCodec[T <: GatewayException]: Codec[T] = gatewayExceptionCodecValue.asInstanceOf[Codec[T]]
  lazy val gatewayExceptionCodecValue: Codec[GatewayException] = {
    @silent implicit lazy val emptyEventKeysCodec: Codec[EmptyEventKeys]                   = deriveCodec
    @silent implicit lazy val eventServerNotAvailableCodec: Codec[EventServerUnavailable]  = deriveCodec
    @silent implicit lazy val invalidComponentCodec: Codec[InvalidComponent]               = deriveUnaryCodec
    @silent implicit lazy val setAlarmSeverityFailureCodec: Codec[SetAlarmSeverityFailure] = deriveUnaryCodec
    @silent implicit lazy val invalidMaxFrequencyCodec: Codec[InvalidMaxFrequency]         = deriveCodec
    deriveCodec
  }

  implicit def postRequestCodec[T <: PostRequest]: Codec[T] = postRequestValue.asInstanceOf[Codec[T]]
  lazy val postRequestValue: Codec[PostRequest] = {
    import esw.gateway.api.protocol.PostRequest._
    @silent implicit lazy val commandPostRequestCodec: Codec[PostRequest.ComponentCommand] = deriveCodec
    @silent implicit lazy val sequencerCommandPostRequestCodec: Codec[SequencerCommand]    = deriveCodec
    @silent implicit lazy val publishEventCodec: Codec[PublishEvent]                       = deriveCodec
    @silent implicit lazy val getEventCodec: Codec[GetEvent]                               = deriveCodec
    @silent implicit lazy val setAlarmSeverityCodec: Codec[SetAlarmSeverity]               = deriveCodec
    @silent implicit lazy val metadataEnc: Encoder[Map[String, Any]]                       = Encoder[MapElem].contramap(ElementConverter.fromMap)
    @silent implicit lazy val metadataDec: Decoder[Map[String, Any]]                       = Decoder[MapElem].map(ElementConverter.toMap)
    @silent implicit lazy val logCodec: Codec[Log]                                         = deriveCodec
    deriveCodec
  }

  implicit def websocketRequestCodec[T <: WebsocketRequest]: Codec[T] = websocketRequestCodecValue.asInstanceOf[Codec[T]]
  lazy val websocketRequestCodecValue: Codec[WebsocketRequest] = {
    import esw.gateway.api.protocol.WebsocketRequest._
    @silent implicit lazy val commandWebsocketRequestCodec: Codec[WebsocketRequest.ComponentCommand] = deriveCodec
    @silent implicit lazy val sequencerCommandWebsocketRequestCodec: Codec[SequencerCommand]         = deriveCodec
    @silent implicit lazy val subscribeCodec: Codec[Subscribe]                                       = deriveCodec
    @silent implicit lazy val subscribeWithPatternCodec: Codec[SubscribeWithPattern]                 = deriveCodec
    deriveCodec
  }

  //Todo: move to csw
  implicit lazy val eventKeyCodec: Codec[EventKey] = deriveCodec
  implicit lazy val alarmKeyCodec: Codec[AlarmKey] = deriveCodec

  implicit lazy val PostRequestErrorProtocol: ErrorProtocol[PostRequest] = ErrorProtocol.bind[PostRequest, GatewayException]
  implicit lazy val WebsocketRequestErrorProtocol: ErrorProtocol[WebsocketRequest] =
    ErrorProtocol.bind[WebsocketRequest, GatewayException]
}

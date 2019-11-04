package esw.gateway.api.codecs

import com.github.ghik.silencer.silent
import csw.alarm.models.Key.AlarmKey
import csw.location.models.codecs.LocationCodecs
import csw.logging.models.codecs.LoggingCodecs
import csw.params.core.formats.ParamCodecs
import csw.params.events.EventKey
import esw.gateway.api.protocol.PostRequest._
import esw.gateway.api.protocol.WebsocketRequest.{QueryFinal, Subscribe, SubscribeCurrentState, SubscribeWithPattern}
import esw.gateway.api.protocol._
import io.bullet.borer.Dom.MapElem
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec
import io.bullet.borer.{Codec, Decoder, Encoder}
import msocket.api.codecs.BasicCodecs

trait GatewayCodecs extends ParamCodecs with LocationCodecs with BasicCodecs with LoggingCodecs {

  implicit def getEventErrorCodec[T <: GetEventError]: Codec[T] = getEventErrorCodecValue.asInstanceOf[Codec[T]]
  lazy val getEventErrorCodecValue: Codec[GetEventError] = {
    @silent implicit lazy val emptyEventKeysCodec: Codec[EmptyEventKeys.type]                  = deriveCodec
    @silent implicit lazy val eventServerNotAvailableCodec: Codec[EventServerUnavailable.type] = deriveCodec
    deriveCodec
  }

  implicit def postRequestCodec[T <: PostRequest]: Codec[T] = postRequestValue.asInstanceOf[Codec[T]]
  lazy val postRequestValue: Codec[PostRequest] = {
    @silent implicit lazy val submitCodec: Codec[Submit]                     = deriveCodec
    @silent implicit lazy val onewayCodec: Codec[Oneway]                     = deriveCodec
    @silent implicit lazy val validateCodec: Codec[Validate]                 = deriveCodec
    @silent implicit lazy val publishEventCodec: Codec[PublishEvent]         = deriveCodec
    @silent implicit lazy val getEventCodec: Codec[GetEvent]                 = deriveCodec
    @silent implicit lazy val setAlarmSeverityCodec: Codec[SetAlarmSeverity] = deriveCodec
    @silent implicit val metadataEnc: Encoder[Map[String, Any]]              = implicitly[Encoder[MapElem]].contramap(ElementConverter.fromMap)
    @silent implicit val metadataDec: Decoder[Map[String, Any]]              = implicitly[Decoder[MapElem]].map(ElementConverter.toMap)
    @silent implicit lazy val logCodec: Codec[Log]                           = deriveCodec
    deriveCodec
  }

  implicit lazy val invalidComponentCodec: Codec[InvalidComponent]               = deriveCodec
  implicit lazy val setAlarmSeverityFailureCodec: Codec[SetAlarmSeverityFailure] = deriveCodec

  implicit def websocketRequestCodec[T <: WebsocketRequest]: Codec[T] = websocketRequestCodecValue.asInstanceOf[Codec[T]]
  lazy val websocketRequestCodecValue: Codec[WebsocketRequest] = {
    @silent implicit lazy val queryFinalCodec: Codec[QueryFinal]                       = deriveCodec
    @silent implicit lazy val subscribeCodec: Codec[Subscribe]                         = deriveCodec
    @silent implicit lazy val subscribeWithPatternCodec: Codec[SubscribeWithPattern]   = deriveCodec
    @silent implicit lazy val subscribeCurrentStateCodec: Codec[SubscribeCurrentState] = deriveCodec
    deriveCodec
  }

  //Todo: move to csw
  implicit lazy val eventKeyCodec: Codec[EventKey] = deriveCodec
  implicit lazy val alarmKeyCodec: Codec[AlarmKey] = deriveCodec
}

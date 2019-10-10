package esw.gateway.api.codecs

import com.github.ghik.silencer.silent
import csw.alarm.codecs.AlarmCodecs
import csw.alarm.models.Key.AlarmKey
import csw.location.api.codec.DoneCodec
import csw.location.models.codecs.LocationCodecs
import csw.logging.models.Level
import csw.logging.models.codecs.LoggingCodecs
import csw.params.core.formats.ParamCodecs
import csw.params.events.EventKey
import esw.gateway.api.protocol.PostRequest._
import esw.gateway.api.protocol.WebsocketRequest.{QueryFinal, Subscribe, SubscribeCurrentState, SubscribeWithPattern}
import esw.gateway.api.protocol._
import io.bullet.borer.Dom.Element
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec
import io.bullet.borer.{Codec, Decoder, Encoder}
import msocket.api.codecs.EitherCodecs

trait GatewayCodecs extends ParamCodecs with LocationCodecs with AlarmCodecs with EitherCodecs with DoneCodec {
  lazy val getEventErrorCodecValue: Codec[GetEventError] = {
    @silent implicit lazy val emptyEventKeysCodec: Codec[EmptyEventKeys.type] = deriveCodec[EmptyEventKeys.type]
    @silent implicit lazy val eventServerNotAvailableCodec: Codec[EventServerUnavailable.type] =
      deriveCodec[EventServerUnavailable.type]
    deriveCodec[GetEventError]
  }

  implicit def getEventErrorCodec[T <: GetEventError]: Codec[T] = getEventErrorCodecValue.asInstanceOf[Codec[T]]
  lazy val postRequestValue: Codec[PostRequest] = {
    @silent implicit lazy val submitCodec: Codec[Submit]                     = deriveCodec[Submit]
    @silent implicit lazy val onewayCodec: Codec[Oneway]                     = deriveCodec[Oneway]
    @silent implicit lazy val validateCodec: Codec[Validate]                 = deriveCodec[Validate]
    @silent implicit lazy val publishEventCodec: Codec[PublishEvent]         = deriveCodec[PublishEvent]
    @silent implicit lazy val getEventCodec: Codec[GetEvent]                 = deriveCodec[GetEvent]
    @silent implicit lazy val setAlarmSeverityCodec: Codec[SetAlarmSeverity] = deriveCodec[SetAlarmSeverity]
    @silent implicit lazy val levelCodec: Codec[Level]                       = LoggingCodecs.levelCodec

    @silent implicit val metadataEnc: Encoder[Map[String, Any]] = {
      implicitly[Encoder[Map[String, Element]]].contramap(
        m =>
          m.view
            .mapValues(ElementConverter.fromAny)
            .filter {
              case (_, v) => v != null
            }
            .toMap
      )
    }

    @silent implicit val metadataDec: Decoder[Map[String, Any]] = {
      implicitly[Decoder[Map[String, Element]]].map(
        m =>
          m.view
            .mapValues(ElementConverter.toAny)
            .filter {
              case (_, v) => v != null
            }
            .toMap
      )
    }

    @silent implicit lazy val logCodec: Codec[Log] = deriveCodec[Log]
    deriveCodec[PostRequest]
  }

  implicit lazy val invalidComponentCodec: Codec[InvalidComponent] = deriveCodec[InvalidComponent]

  implicit lazy val setAlarmSeverityFailureCodec: Codec[SetAlarmSeverityFailure] = deriveCodec[SetAlarmSeverityFailure]

  implicit def postRequestCodec[T <: PostRequest]: Codec[T] = postRequestValue.asInstanceOf[Codec[T]]
  lazy val websocketRequestCodecValue: Codec[WebsocketRequest] = {
    @silent implicit lazy val queryFinalCodec: Codec[QueryFinal] = deriveCodec[QueryFinal]
    @silent implicit lazy val subscribeCodec: Codec[Subscribe]   = deriveCodec[Subscribe]
    @silent implicit lazy val subscribeWithPatternCodec: Codec[SubscribeWithPattern] =
      deriveCodec[SubscribeWithPattern]
    @silent implicit lazy val subscribeCurrentStateCodec: Codec[SubscribeCurrentState] =
      deriveCodec[SubscribeCurrentState]

    deriveCodec[WebsocketRequest]
  }

  def singletonErrorCodec[T <: SingletonError with Singleton](a: T): Codec[T] = Codec.bimap[String, T](_.msg, _ => a)

  implicit def websocketRequestCodec[T <: WebsocketRequest]: Codec[T] =
    websocketRequestCodecValue.asInstanceOf[Codec[T]]

  //Todo: move to csw
  implicit lazy val eventKeyCodec: Codec[EventKey] = deriveCodec[EventKey]
  implicit lazy val alarmKeyCodec: Codec[AlarmKey] = deriveCodec[AlarmKey]
}

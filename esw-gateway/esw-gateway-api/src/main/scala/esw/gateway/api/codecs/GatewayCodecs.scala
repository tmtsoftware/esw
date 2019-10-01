package esw.gateway.api.codecs

import com.github.ghik.silencer.silent
import csw.alarm.codecs.AlarmCodecs
import csw.alarm.models.Key.AlarmKey
import csw.location.api.codec.DoneCodec
import csw.location.models.codecs.LocationCodecs
import csw.params.core.formats.ParamCodecs
import csw.params.events.EventKey
import esw.gateway.api.protocol.PostRequest._
import esw.gateway.api.protocol.WebsocketRequest.{QueryFinal, Subscribe, SubscribeCurrentState, SubscribeWithPattern}
import esw.gateway.api.protocol._
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec
import msocket.api.codecs.EitherCodecs

trait GatewayCodecs extends ParamCodecs with LocationCodecs with AlarmCodecs with EitherCodecs with DoneCodec {

  def singletonErrorCodec[T <: SingletonError with Singleton](a: T): Codec[T] = Codec.bimap[String, T](_.msg, _ => a)

  implicit def getEventErrorCodec[T <: GetEventError]: Codec[T] = getEventErrorCodecValue.asInstanceOf[Codec[T]]
  lazy val getEventErrorCodecValue: Codec[GetEventError] = {
    @silent implicit lazy val emptyEventKeysCodec: Codec[EmptyEventKeys.type] = singletonCodec(EmptyEventKeys)
    @silent implicit lazy val eventServerNotAvailableCodec: Codec[EventServerUnavailable.type] =
      singletonCodec(EventServerUnavailable)
    deriveCodec[GetEventError]
  }

  implicit lazy val invalidComponentCodec: Codec[InvalidComponent] = deriveCodec[InvalidComponent]

  implicit lazy val setAlarmSeverityFailureCodec: Codec[SetAlarmSeverityFailure] = deriveCodec[SetAlarmSeverityFailure]

  implicit def postRequestCodec[T <: PostRequest]: Codec[T] = postRequestValue.asInstanceOf[Codec[T]]
  lazy val postRequestValue: Codec[PostRequest] = {
    @silent implicit lazy val submitCodec: Codec[Submit]                     = deriveCodec[Submit]
    @silent implicit lazy val onewayCodec: Codec[Oneway]                     = deriveCodec[Oneway]
    @silent implicit lazy val validateCodec: Codec[Validate]                 = deriveCodec[Validate]
    @silent implicit lazy val publishEventCodec: Codec[PublishEvent]         = deriveCodec[PublishEvent]
    @silent implicit lazy val getEventCodec: Codec[GetEvent]                 = deriveCodec[GetEvent]
    @silent implicit lazy val setAlarmSeverityCodec: Codec[SetAlarmSeverity] = deriveCodec[SetAlarmSeverity]
    deriveCodec[PostRequest]
  }

  implicit def websocketRequestCodec[T <: WebsocketRequest]: Codec[T] =
    websocketRequestCodecValue.asInstanceOf[Codec[T]]
  lazy val websocketRequestCodecValue: Codec[WebsocketRequest] = {
    @silent implicit lazy val queryFinalCodec: Codec[QueryFinal] = deriveCodec[QueryFinal]
    @silent implicit lazy val subscribeCodec: Codec[Subscribe]   = deriveCodec[Subscribe]
    @silent implicit lazy val subscribeWithPatternCodec: Codec[SubscribeWithPattern] =
      deriveCodec[SubscribeWithPattern]
    @silent implicit lazy val subscribeCurrentStateCodec: Codec[SubscribeCurrentState] =
      deriveCodec[SubscribeCurrentState]

    deriveCodec[WebsocketRequest]
  }

  //Todo: move to csw
  implicit lazy val eventKeyCodec: Codec[EventKey] = deriveCodec[EventKey]
  implicit lazy val alarmKeyCodec: Codec[AlarmKey] = deriveCodec[AlarmKey]
}

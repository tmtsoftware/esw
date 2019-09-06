package esw.gateway.api.codecs

import com.github.ghik.silencer.silent
import csw.alarm.codecs.AlarmCodecs
import csw.alarm.models.Key.AlarmKey
import csw.location.api.codec.DoneCodec
import csw.location.models.codecs.LocationCodecs
import csw.params.core.formats.{CodecHelpers, ParamCodecs}
import csw.params.events.EventKey
import esw.gateway.api.messages.PostRequest._
import esw.gateway.api.messages.WebsocketRequest.{QueryFinal, Subscribe, SubscribeCurrentState, SubscribeWithPattern}
import esw.gateway.api.messages._
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec
import msocket.api.utils.EitherCodecs

trait RestlessCodecs extends ParamCodecs with LocationCodecs with AlarmCodecs with EitherCodecs with DoneCodec {

  def singletonErrorCodec[T <: SingletonError with Singleton](a: T): Codec[T] =
    Codec.bimap[String, T](_.msg, _ => a)

  implicit def eventErrorCodec[T <: EventError]: Codec[T] = eventErrorCodecValue.asInstanceOf[Codec[T]]
  lazy val eventErrorCodecValue: Codec[EventError] = {
    @silent implicit def getEventErrorCodec: Codec[GetEventError]             = deriveCodec[GetEventError]
    @silent implicit lazy val emptyEventKeysCodec: Codec[EmptyEventKeys.type] = singletonCodec(EmptyEventKeys)
    @silent implicit lazy val eventServerNotAvailableCodec: Codec[EventServerUnavailable.type] =
      singletonCodec(EventServerUnavailable)
    invalidMaxFrequencyCodec
    deriveCodec[EventError]
  }

  implicit def commandErrorMsgCodec[T <: CommandError]: Codec[T] = commandErrorMsgCodecValue.asInstanceOf[Codec[T]]
  lazy val commandErrorMsgCodecValue: Codec[CommandError] = {
    @silent implicit lazy val invalidComponentCodec: Codec[InvalidComponent] = deriveCodec[InvalidComponent]
    invalidMaxFrequencyCodec
    deriveCodec[CommandError]
  }

  implicit lazy val invalidMaxFrequencyCodec: Codec[InvalidMaxFrequency.type]    = singletonCodec(InvalidMaxFrequency)
  implicit lazy val setAlarmSeverityFailureCodec: Codec[SetAlarmSeverityFailure] = deriveCodec[SetAlarmSeverityFailure]

  implicit def postRequestCodec[T <: PostRequest]: Codec[T] = postRequestValue.asInstanceOf[Codec[T]]
  lazy val postRequestValue: Codec[PostRequest] = {
    @silent implicit lazy val commandRequestCodec: Codec[CommandRequest]     = deriveCodec[CommandRequest]
    @silent implicit lazy val publishEventCodec: Codec[PublishEvent]         = deriveCodec[PublishEvent]
    @silent implicit lazy val getEventCodec: Codec[GetEvent]                 = deriveCodec[GetEvent]
    @silent implicit lazy val setAlarmSeverityCodec: Codec[SetAlarmSeverity] = deriveCodec[SetAlarmSeverity]
    deriveCodec[PostRequest]
  }
  implicit lazy val commandActionCodec: Codec[CommandAction] = CodecHelpers.enumCodec[CommandAction]

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

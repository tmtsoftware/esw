package esw.gateway.server.routes.restless.codecs

import com.github.ghik.silencer.silent
import csw.alarm.codecs.AlarmCodecs
import csw.location.client.HttpCodecs
import csw.location.models.codecs.LocationCodecs
import csw.params.core.formats.{CodecHelpers, ParamCodecs}
import esw.gateway.server.routes.restless.messages.ErrorResponseMsg._
import esw.gateway.server.routes.restless.messages.HttpRequestMsg._
import esw.gateway.server.routes.restless.messages.WebSocketMsg.{
  CurrentStateSubscriptionCommandMsg,
  PatternSubscribeEventMsg,
  QueryCommandMsg,
  SubscribeEventMsg
}
import esw.gateway.server.routes.restless.messages._
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec

trait RestlessCodecs extends ParamCodecs with LocationCodecs with HttpCodecs with AlarmCodecs with EitherCodecs {
  implicit def responseMsgCodec[T <: ErrorResponseMsg]: Codec[T] = responseMsgCodecValue.asInstanceOf[Codec[T]]

  lazy val responseMsgCodecValue: Codec[ErrorResponseMsg] = {
    @silent implicit lazy val noEventKeysCodec: Codec[EmptyEventKeys]                      = deriveCodec[EmptyEventKeys]
    @silent implicit lazy val invalidComponentCodec: Codec[InvalidComponent]               = deriveCodec[InvalidComponent]
    @silent implicit lazy val invalidMaxFrequencyCodec: Codec[InvalidMaxFrequency]         = deriveCodec[InvalidMaxFrequency]
    @silent implicit lazy val setAlarmSeverityFailureCodec: Codec[SetAlarmSeverityFailure] = deriveCodec[SetAlarmSeverityFailure]
    deriveCodec[ErrorResponseMsg]
  }

  implicit def requestMsgCodec[T <: HttpRequestMsg]: Codec[T] = requestMsgCodecValue.asInstanceOf[Codec[T]]

  lazy val requestMsgCodecValue: Codec[HttpRequestMsg] = {
    @silent implicit lazy val commandMsgCodec: Codec[CommandMsg]                   = deriveCodec[CommandMsg]
    @silent implicit lazy val publishEventMsgCodec: Codec[PublishEventMsg]         = deriveCodec[PublishEventMsg]
    @silent implicit lazy val getEventMsgCodec: Codec[GetEventMsg]                 = deriveCodec[GetEventMsg]
    @silent implicit lazy val setAlarmSeverityMsgCodec: Codec[SetAlarmSeverityMsg] = deriveCodec[SetAlarmSeverityMsg]
    deriveCodec[HttpRequestMsg]
  }

  implicit def websocketMsgCodec[T <: WebSocketMsg]: Codec[T] = requestMsgCodecValue.asInstanceOf[Codec[T]]

  lazy val webSocketMsgCodecValue: Codec[WebSocketMsg] = {
    @silent implicit lazy val queryCommandMsgCodec: Codec[QueryCommandMsg]     = deriveCodec[QueryCommandMsg]
    @silent implicit lazy val subscribeEventMsgCodec: Codec[SubscribeEventMsg] = deriveCodec[SubscribeEventMsg]
    @silent implicit lazy val patternSubscribeEventMsgCodec: Codec[PatternSubscribeEventMsg] =
      deriveCodec[PatternSubscribeEventMsg]
    @silent implicit lazy val currentStateSubscriptionCommandMsgCodec: Codec[CurrentStateSubscriptionCommandMsg] =
      deriveCodec[CurrentStateSubscriptionCommandMsg]

    deriveCodec[WebSocketMsg]
  }

  implicit lazy val commandActionCode: Codec[CommandAction] = CodecHelpers.enumCodec[CommandAction]

}

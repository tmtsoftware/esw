package esw.gateway.server.routes.restless.codecs

import com.github.ghik.silencer.silent
import csw.alarm.codecs.AlarmCodecs
import csw.location.models.codecs.LocationCodecs
import csw.params.core.formats.{CodecHelpers, ParamCodecs}
import csw.params.events.EventKey
import esw.gateway.server.routes.restless.messages.GatewayMessage._
import esw.gateway.server.routes.restless.messages.WebSocketMessage.{
  CurrentStateSubscriptionCommandMessage,
  PatternSubscribeEventMessage,
  QueryCommandMessage,
  SubscribeEventMessage
}
import esw.gateway.server.routes.restless.messages._
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec

trait RestlessCodecs extends ParamCodecs with LocationCodecs with AlarmCodecs with EitherCodecs {
  implicit def eventErrorMsgCodec[T <: EventErrorMessage]: Codec[T] = eventErrorMsgCodecValue.asInstanceOf[Codec[T]]

  lazy val eventErrorMsgCodecValue: Codec[EventErrorMessage] = {
    @silent implicit lazy val noEventKeysCodec: Codec[EmptyEventKeys] = deriveCodec[EmptyEventKeys]
    invalidMaxFrequencyCodec
    deriveCodec[EventErrorMessage]
  }

  implicit def commandErrorMsgCodec[T <: CommandErrorMessage]: Codec[T] = eventErrorMsgCodecValue.asInstanceOf[Codec[T]]

  lazy val commandErrorMsgCodecValue: Codec[CommandErrorMessage] = {
    @silent implicit lazy val invalidComponentCodec: Codec[InvalidComponent] = deriveCodec[InvalidComponent]
    invalidMaxFrequencyCodec
    deriveCodec[CommandErrorMessage]
  }

  implicit lazy val invalidMaxFrequencyCodec: Codec[InvalidMaxFrequency] = deriveCodec[InvalidMaxFrequency]

  implicit def alarmErrorMsgCodec[T <: AlarmErrorMessage]: Codec[T] = eventErrorMsgCodecValue.asInstanceOf[Codec[T]]

  lazy val alarmErrorMsgCodecValue: Codec[AlarmErrorMessage] = {
    @silent implicit lazy val setAlarmSeverityFailureCodec: Codec[SetAlarmSeverityFailure] = deriveCodec[SetAlarmSeverityFailure]
    deriveCodec[AlarmErrorMessage]
  }

  implicit def gatewayMsgCodec[T <: GatewayMessage]: Codec[T] = gatewayMsgCodecValue.asInstanceOf[Codec[T]]

  lazy val gatewayMsgCodecValue: Codec[GatewayMessage] = {
    @silent implicit lazy val commandMsgCodec: Codec[CommandMessage]                   = deriveCodec[CommandMessage]
    @silent implicit lazy val publishEventMsgCodec: Codec[PublishEventMessage]         = deriveCodec[PublishEventMessage]
    @silent implicit lazy val getEventMsgCodec: Codec[GetEventMessage]                 = deriveCodec[GetEventMessage]
    @silent implicit lazy val setAlarmSeverityMsgCodec: Codec[SetAlarmSeverityMessage] = deriveCodec[SetAlarmSeverityMessage]
    deriveCodec[GatewayMessage]
  }

  implicit def websocketMsgCodec[T <: WebSocketMessage]: Codec[T] = gatewayMsgCodecValue.asInstanceOf[Codec[T]]

  lazy val webSocketMsgCodecValue: Codec[WebSocketMessage] = {
    @silent implicit lazy val queryCommandMsgCodec: Codec[QueryCommandMessage]     = deriveCodec[QueryCommandMessage]
    @silent implicit lazy val subscribeEventMsgCodec: Codec[SubscribeEventMessage] = deriveCodec[SubscribeEventMessage]
    @silent implicit lazy val patternSubscribeEventMsgCodec: Codec[PatternSubscribeEventMessage] =
      deriveCodec[PatternSubscribeEventMessage]
    @silent implicit lazy val currentStateSubscriptionCommandMsgCodec: Codec[CurrentStateSubscriptionCommandMessage] =
      deriveCodec[CurrentStateSubscriptionCommandMessage]

    deriveCodec[WebSocketMessage]
  }

  implicit lazy val commandActionCodec: Codec[CommandAction] = CodecHelpers.enumCodec[CommandAction]

  implicit lazy val eventKeyCodec: Codec[EventKey] = deriveCodec[EventKey]

}

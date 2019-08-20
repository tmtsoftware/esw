package esw.gateway.server.routes.restless.codecs

import com.github.ghik.silencer.silent
import csw.alarm.codecs.AlarmCodecs
import csw.location.models.codecs.LocationCodecs
import csw.params.core.formats.{CodecHelpers, ParamCodecs}
import csw.params.events.EventKey
import esw.gateway.server.routes.restless.messages.GatewayRequest._
import esw.gateway.server.routes.restless.messages.WebSocketRequest.{
  QueryFinal,
  Subscribe,
  SubscribeCurrentState,
  SubscribeWithPattern
}
import esw.gateway.server.routes.restless.messages._
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec

trait RestlessCodecs extends ParamCodecs with LocationCodecs with AlarmCodecs with EitherCodecs {

  implicit def eventErrorMsgCodec[T <: EventError]: Codec[T] = eventErrorMsgCodecValue.asInstanceOf[Codec[T]]
  lazy val eventErrorMsgCodecValue: Codec[EventError] = {
    @silent implicit lazy val noEventKeysCodec: Codec[EmptyEventKeys] = deriveCodec[EmptyEventKeys]
    invalidMaxFrequencyCodec
    deriveCodec[EventError]
  }

  implicit def commandErrorMsgCodec[T <: CommandError]: Codec[T] = eventErrorMsgCodecValue.asInstanceOf[Codec[T]]
  lazy val commandErrorMsgCodecValue: Codec[CommandError] = {
    @silent implicit lazy val invalidComponentCodec: Codec[InvalidComponent] = deriveCodec[InvalidComponent]
    invalidMaxFrequencyCodec
    deriveCodec[CommandError]
  }

  implicit lazy val invalidMaxFrequencyCodec: Codec[InvalidMaxFrequency]         = deriveCodec[InvalidMaxFrequency]
  implicit lazy val setAlarmSeverityFailureCodec: Codec[SetAlarmSeverityFailure] = deriveCodec[SetAlarmSeverityFailure]

  implicit def gatewayMsgCodec[T <: GatewayRequest]: Codec[T] = gatewayMsgCodecValue.asInstanceOf[Codec[T]]
  lazy val gatewayMsgCodecValue: Codec[GatewayRequest] = {
    @silent implicit lazy val commandMsgCodec: Codec[CommandRequest]            = deriveCodec[CommandRequest]
    @silent implicit lazy val publishEventMsgCodec: Codec[PublishEvent]         = deriveCodec[PublishEvent]
    @silent implicit lazy val getEventMsgCodec: Codec[GetEvent]                 = deriveCodec[GetEvent]
    @silent implicit lazy val setAlarmSeverityMsgCodec: Codec[SetAlarmSeverity] = deriveCodec[SetAlarmSeverity]
    deriveCodec[GatewayRequest]
  }
  implicit lazy val commandActionCodec: Codec[CommandAction] = CodecHelpers.enumCodec[CommandAction]

  implicit def websocketMsgCodec[T <: WebSocketRequest]: Codec[T] = gatewayMsgCodecValue.asInstanceOf[Codec[T]]
  lazy val webSocketMsgCodecValue: Codec[WebSocketRequest] = {
    @silent implicit lazy val queryCommandMsgCodec: Codec[QueryFinal]  = deriveCodec[QueryFinal]
    @silent implicit lazy val subscribeEventMsgCodec: Codec[Subscribe] = deriveCodec[Subscribe]
    @silent implicit lazy val patternSubscribeEventMsgCodec: Codec[SubscribeWithPattern] =
      deriveCodec[SubscribeWithPattern]
    @silent implicit lazy val currentStateSubscriptionCommandMsgCodec: Codec[SubscribeCurrentState] =
      deriveCodec[SubscribeCurrentState]

    deriveCodec[WebSocketRequest]
  }

  implicit lazy val eventKeyCodec: Codec[EventKey] = deriveCodec[EventKey]

}

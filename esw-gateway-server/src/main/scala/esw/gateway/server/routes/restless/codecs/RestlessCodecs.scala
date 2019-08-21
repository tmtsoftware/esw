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

  implicit def eventErrorCodec[T <: EventError]: Codec[T] = eventErrorCodecValue.asInstanceOf[Codec[T]]
  lazy val eventErrorCodecValue: Codec[EventError] = {
    @silent implicit lazy val emptyEventKeysCodec: Codec[EmptyEventKeys] = deriveCodec[EmptyEventKeys]
    invalidMaxFrequencyCodec
    deriveCodec[EventError]
  }

  implicit def commandErrorMsgCodec[T <: CommandError]: Codec[T] = eventErrorCodecValue.asInstanceOf[Codec[T]]
  lazy val commandErrorMsgCodecValue: Codec[CommandError] = {
    @silent implicit lazy val invalidComponentCodec: Codec[InvalidComponent] = deriveCodec[InvalidComponent]
    invalidMaxFrequencyCodec
    deriveCodec[CommandError]
  }

  implicit lazy val invalidMaxFrequencyCodec: Codec[InvalidMaxFrequency]         = deriveCodec[InvalidMaxFrequency]
  implicit lazy val setAlarmSeverityFailureCodec: Codec[SetAlarmSeverityFailure] = deriveCodec[SetAlarmSeverityFailure]

  implicit def gatewayRequestCodec[T <: GatewayRequest]: Codec[T] = gatewayRequestValue.asInstanceOf[Codec[T]]
  lazy val gatewayRequestValue: Codec[GatewayRequest] = {
    @silent implicit lazy val commandRequestCodec: Codec[CommandRequest]     = deriveCodec[CommandRequest]
    @silent implicit lazy val publishEventCodec: Codec[PublishEvent]         = deriveCodec[PublishEvent]
    @silent implicit lazy val getEventCodec: Codec[GetEvent]                 = deriveCodec[GetEvent]
    @silent implicit lazy val setAlarmSeverityCodec: Codec[SetAlarmSeverity] = deriveCodec[SetAlarmSeverity]
    deriveCodec[GatewayRequest]
  }
  implicit lazy val commandActionCodec: Codec[CommandAction] = CodecHelpers.enumCodec[CommandAction]

  implicit def websocketMsgCodec[T <: WebSocketRequest]: Codec[T] = webSocketRequestCodecValue.asInstanceOf[Codec[T]]
  lazy val webSocketRequestCodecValue: Codec[WebSocketRequest] = {
    @silent implicit lazy val queryFinalCodec: Codec[QueryFinal] = deriveCodec[QueryFinal]
    @silent implicit lazy val subscribeCodec: Codec[Subscribe]   = deriveCodec[Subscribe]
    @silent implicit lazy val subscribeWithPatternCodec: Codec[SubscribeWithPattern] =
      deriveCodec[SubscribeWithPattern]
    @silent implicit lazy val subscribeCurrentStateCodec: Codec[SubscribeCurrentState] =
      deriveCodec[SubscribeCurrentState]

    deriveCodec[WebSocketRequest]
  }

  implicit lazy val eventKeyCodec: Codec[EventKey] = deriveCodec[EventKey]

}

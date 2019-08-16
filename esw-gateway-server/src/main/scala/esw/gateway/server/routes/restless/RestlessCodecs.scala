package esw.gateway.server.routes.restless
import csw.alarm.codecs.AlarmCodecs
import csw.location.client.HttpCodecs
import csw.location.models.codecs.LocationCodecs
import csw.params.core.formats.{CodecHelpers, ParamCodecs}
import esw.gateway.server.routes.restless.ResponseMsg.{NoEventKeys, SetAlarmSeverityFailure}
import esw.gateway.server.routes.restless.RoutesMsg._
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs._

trait RestlessCodecs extends ParamCodecs with LocationCodecs with HttpCodecs with AlarmCodecs {

  implicit def responseMsgCodec[T <: ResponseMsg]: Codec[T] = {
    implicit lazy val noEventKeysCodec: Codec[NoEventKeys.type]                    = singletonCodec(NoEventKeys)
    implicit lazy val setAlarmSeverityFailureCodec: Codec[SetAlarmSeverityFailure] = deriveCodec[SetAlarmSeverityFailure]
    deriveCodec[ResponseMsg].asInstanceOf[Codec[T]]
  }

  implicit def routeMsgCodec[T <: RouteMsg]: Codec[T] = {
    implicit lazy val commandMsgCodec: Codec[CommandMsg]                   = deriveCodec[CommandMsg]
    implicit lazy val publishEventMsgCodec: Codec[PublishEventMsg]         = deriveCodec[PublishEventMsg]
    implicit lazy val getEventMsgCodec: Codec[GetEventMsg]                 = deriveCodec[GetEventMsg]
    implicit lazy val setAlarmSeverityMsgCodec: Codec[SetAlarmSeverityMsg] = deriveCodec[SetAlarmSeverityMsg]
    deriveCodec[RouteMsg].asInstanceOf[Codec[T]]
  }

  implicit lazy val commandActionCode: Codec[CommandAction] = CodecHelpers.enumCodec[CommandAction]

}

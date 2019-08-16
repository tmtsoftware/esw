package esw.gateway.server.routes.restless
import com.github.ghik.silencer.silent
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
    @silent implicit lazy val noEventKeysCodec: Codec[NoEventKeys.type]                    = singletonCodec(NoEventKeys)
    @silent implicit lazy val setAlarmSeverityFailureCodec: Codec[SetAlarmSeverityFailure] = deriveCodec[SetAlarmSeverityFailure]
    deriveCodec[ResponseMsg].asInstanceOf[Codec[T]]
  }

  implicit def routeMsgCodec[T <: RouteMsg]: Codec[T] = {
    @silent implicit lazy val commandMsgCodec: Codec[CommandMsg]                   = deriveCodec[CommandMsg]
    @silent implicit lazy val publishEventMsgCodec: Codec[PublishEventMsg]         = deriveCodec[PublishEventMsg]
    @silent implicit lazy val getEventMsgCodec: Codec[GetEventMsg]                 = deriveCodec[GetEventMsg]
    @silent implicit lazy val setAlarmSeverityMsgCodec: Codec[SetAlarmSeverityMsg] = deriveCodec[SetAlarmSeverityMsg]
    deriveCodec[RouteMsg].asInstanceOf[Codec[T]]
  }

  implicit lazy val commandActionCode: Codec[CommandAction] = CodecHelpers.enumCodec[CommandAction]

}

package esw.gateway.server.routes.restless
import csw.params.core.formats.CodecHelpers
import esw.gateway.server.routes.restless.ResponseMsg.{NoEventKeys, SetAlarmSeverityFailure}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs._

trait RestlessCodecs {

  def singletonCodec[T <: Singleton](a: T): Codec[T] = CodecHelpers.bimap[String, T](_ => a, _.toString)

  implicit def codec[T <: ResponseMsg]: Codec[T] = {
    implicit lazy val noEventKeysCodec: Codec[NoEventKeys.type]                    = singletonCodec(NoEventKeys)
    implicit lazy val SetAlarmSeverityFailureCodec: Codec[SetAlarmSeverityFailure] = deriveCodec[SetAlarmSeverityFailure]
    deriveCodec[ResponseMsg].asInstanceOf[Codec[T]]
  }

//  implicit lazy val CommandActionFailureCodec: Codec[CommandActionFailure] = deriveCodec[CommandActionFailure]

}

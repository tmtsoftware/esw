package esw.gateway.server.requests

import csw.alarm.codecs.AlarmCodecs
import csw.alarm.models.AlarmSeverity
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs._

case class SetSeverity(severity: AlarmSeverity)

object SetSeverity extends AlarmCodecs {
  implicit val codec: Codec[SetSeverity] = deriveCodec[SetSeverity]
}

package esw.ocs.impl.messages

import esw.ocs.api.codecs.OcsAkkaSerializable

sealed trait HealthCheckMsg extends OcsAkkaSerializable

object HealthCheckMsg {
  case object StartHealthCheck  extends HealthCheckMsg
  case object RaiseNotification extends HealthCheckMsg
  case object SendHeartbeat     extends HealthCheckMsg
}

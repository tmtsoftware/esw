package esw.ocs.impl.messages

import esw.ocs.api.codecs.OcsAkkaSerializable

sealed trait HeartbeatActorMsg extends OcsAkkaSerializable

object HeartbeatActorMsg {
  case object StartHeartbeat     extends HeartbeatActorMsg
  case object RaiseNotification  extends HeartbeatActorMsg
  case object PacifyNotification extends HeartbeatActorMsg
}

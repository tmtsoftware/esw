package esw.ocs.impl

import java.time.Duration

import akka.actor.typed.ActorRef
import esw.ocs.impl.messages.HeartbeatActorMsg
import esw.ocs.impl.messages.HeartbeatActorMsg._

class HeartbeatActorProxy(actorRef: ActorRef[HeartbeatActorMsg], val heartbeatInterval: Duration) {
  def pacifyNotification(): Unit = actorRef ! PacifyNotification
  def raiseNotification(): Unit  = actorRef ! RaiseNotification
  def startHeartbeat(): Unit     = actorRef ! StartHeartbeat
}

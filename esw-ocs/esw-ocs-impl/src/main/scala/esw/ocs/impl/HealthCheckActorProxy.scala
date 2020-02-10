package esw.ocs.impl

import java.time.Duration

import akka.actor.typed.ActorRef
import esw.ocs.impl.messages.HealthCheckMsg
import esw.ocs.impl.messages.HealthCheckMsg._

class HealthCheckActorProxy(actorRef: ActorRef[HealthCheckMsg], val heartbeatInterval: Duration) {
  def sendHeartbeat(): Unit    = actorRef ! SendHeartbeat
  def startHealthCheck(): Unit = actorRef ! StartHealthCheck
}

package esw.ocs.impl.internal

import java.time.Duration

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import csw.logging.api.scaladsl.Logger
import scala.compat.java8.DurationConverters.DurationOps
import esw.ocs.impl.messages.HeartbeatActorMsg
import esw.ocs.impl.messages.HeartbeatActorMsg.{PacifyNotification, RaiseNotification, StartHeartbeat}

import scala.concurrent.duration.{DurationLong, FiniteDuration}

class HeartbeatActor(log: Logger, heartbeatDuration: Duration) {

  val jitter: FiniteDuration                      = 10.millis
  val intervalToRaiseNotification: FiniteDuration = heartbeatDuration.toScala + jitter

  def init: Behavior[HeartbeatActorMsg] = Behaviors.withTimers { timerScheduler =>
    Behaviors.receiveMessagePartial[HeartbeatActorMsg] {
      case StartHeartbeat =>
        timerScheduler.startTimerWithFixedDelay(RaiseNotification, intervalToRaiseNotification)
        started(shouldRaiseNotification = true)
    }
  }

  def started(shouldRaiseNotification: Boolean): Behaviors.Receive[HeartbeatActorMsg] =
    Behaviors.receiveMessagePartial[HeartbeatActorMsg] {
      case RaiseNotification if shouldRaiseNotification =>
        log.debug("StrandEc is taking more time than expected")
        Behaviors.same
      case RaiseNotification  => started(shouldRaiseNotification = true)
      case PacifyNotification => started(shouldRaiseNotification = false)
    }
}

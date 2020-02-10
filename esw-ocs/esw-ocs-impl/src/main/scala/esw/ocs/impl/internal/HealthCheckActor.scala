package esw.ocs.impl.internal

import java.time.Duration

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import csw.logging.api.scaladsl.Logger
import esw.ocs.impl.messages.HealthCheckMsg
import esw.ocs.impl.messages.HealthCheckMsg.{RaiseNotification, SendHeartbeat, StartHealthCheck}

import scala.compat.java8.DurationConverters.DurationOps
import scala.concurrent.duration.{DurationLong, FiniteDuration}

class HealthCheckActor(log: Logger, heartbeatDuration: Duration) {

  val jitter: FiniteDuration                      = 10.millis
  val intervalToRaiseNotification: FiniteDuration = heartbeatDuration.toScala + jitter

  def init: Behavior[HealthCheckMsg] = Behaviors.withTimers { timerScheduler =>
    Behaviors.receiveMessagePartial[HealthCheckMsg] {
      case StartHealthCheck =>
        timerScheduler.startTimerWithFixedDelay(RaiseNotification, intervalToRaiseNotification)
        started(shouldRaiseNotification = true)
    }
  }

  def started(shouldRaiseNotification: Boolean): Behaviors.Receive[HealthCheckMsg] =
    Behaviors.receiveMessagePartial[HealthCheckMsg] {
      case RaiseNotification if shouldRaiseNotification =>
        log.debug("StrandEc is taking more time than expected")
        Behaviors.same
      case RaiseNotification => started(shouldRaiseNotification = true)
      case SendHeartbeat     => started(shouldRaiseNotification = false)
    }
}

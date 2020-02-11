package esw.ocs.impl.internal

import java.time.Duration

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import csw.logging.api.scaladsl.Logger
import esw.ocs.impl.messages.HealthCheckMsg
import esw.ocs.impl.messages.HealthCheckMsg.{HeartbeatMissed, Heartbeat}

import scala.compat.java8.DurationConverters.DurationOps
import scala.concurrent.duration.{DurationLong, FiniteDuration}

class HealthCheckActor(log: Logger, heartbeatInterval: Duration) {

  val jitter: FiniteDuration                      = 10.millis
  val intervalToRaiseNotification: FiniteDuration = heartbeatInterval.toScala + jitter

  def behavior(): Behavior[HealthCheckMsg] = Behaviors.setup { ctx =>
    ctx.setReceiveTimeout(intervalToRaiseNotification, HeartbeatMissed)
    Behaviors.receiveMessagePartial[HealthCheckMsg] {
      case HeartbeatMissed =>
        log.error("StrandEc is taking more time than expected")
        Behaviors.same
      case Heartbeat =>
        log.debug("StrandEc heartbeat received")
        Behaviors.same
    }
  }
}

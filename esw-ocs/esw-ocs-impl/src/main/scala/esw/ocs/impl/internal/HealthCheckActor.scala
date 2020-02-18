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
        log.error(
          "[StrandEC Heartbeat Delayed] - Scheduled sending of heartbeat was delayed. " +
            "The reason can be thread starvation, e.g. by running blocking tasks in sequencer script, CPU overload, or GC."
        )
        Behaviors.same
      case Heartbeat =>
        log.info("[StrandEC Heartbeat Received]")
        Behaviors.same
    }
  }
}

package esw.dsl

import java.time.Duration

import csw.time.core.models.{TMTTime, UTCTime}
import csw.time.scheduler.TimeServiceSchedulerFactory
import csw.time.scheduler.api.Cancellable

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

trait TimeServiceDsl {
  private[esw] def timeServiceSchedulerFactory: TimeServiceSchedulerFactory

  def scheduleOnce(startTime: TMTTime)(task: => Unit)(implicit ec: ExecutionContext): Cancellable =
    timeServiceSchedulerFactory.make().scheduleOnce(startTime)(task)

  def schedulePeriodically(interval: FiniteDuration, startTime: TMTTime = UTCTime.now())(
      task: => Unit
  )(implicit ec: ExecutionContext): Cancellable =
    timeServiceSchedulerFactory.make().schedulePeriodically(startTime, Duration.ofNanos(interval.toNanos))(task)
}

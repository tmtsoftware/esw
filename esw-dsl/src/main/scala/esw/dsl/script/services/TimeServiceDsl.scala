package esw.dsl.script.services

import java.time.Duration

import csw.time.core.models.{TMTTime, UTCTime}
import csw.time.scheduler.TimeServiceSchedulerFactory
import csw.time.scheduler.api.Cancellable

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

// fixme: these are callback based api's which need to be run on strand ec
//  can we make it more clear by taking strandEc as implicit instead of just ec?
//  refer EventServiceDsl callback based apis
trait TimeServiceDsl {
  private[esw] def timeServiceSchedulerFactory: TimeServiceSchedulerFactory

  def scheduleOnce(startTime: TMTTime)(task: => Unit)(implicit ec: ExecutionContext): Cancellable =
    timeServiceSchedulerFactory.make().scheduleOnce(startTime)(task)

  def schedulePeriodically(interval: FiniteDuration, startTime: TMTTime = UTCTime.now())(
      task: => Unit
  )(implicit ec: ExecutionContext): Cancellable =
    timeServiceSchedulerFactory.make().schedulePeriodically(startTime, Duration.ofNanos(interval.toNanos))(task)
}

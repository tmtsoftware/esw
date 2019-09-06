package esw.highlevel.dsl.javadsl

import java.time.Duration

import csw.time.core.models.TMTTime
import csw.time.scheduler.api.Cancellable
import esw.highlevel.dsl.TimeServiceDsl

import scala.concurrent.ExecutionContext

trait JTimeServiceDsl { self: TimeServiceDsl =>

  def scheduleOnce(startTime: TMTTime, task: Runnable, ec: ExecutionContext): Cancellable =
    timeServiceSchedulerFactory.make()(ec).scheduleOnce(startTime, task)

  def schedulePeriodically(startTime: TMTTime, interval: Duration, task: Runnable, ec: ExecutionContext): Cancellable =
    timeServiceSchedulerFactory.make()(ec).schedulePeriodically(startTime, interval, task)

}

package esw.highlevel.dsl

import java.time.Duration

import akka.actor.Scheduler
import csw.time.core.models.{TMTTime, UTCTime}
import csw.time.scheduler.TimeServiceSchedulerFactory
import csw.time.scheduler.api.{Cancellable, TimeServiceScheduler}
import esw.ocs.api.BaseTestSuite
import esw.ocs.macros.StrandEc
import org.mockito.ArgumentMatchers.{any, eq => argsEq}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class TimeServiceDslTest extends BaseTestSuite with TimeServiceDsl {

  implicit val scheduler: Scheduler         = mock[Scheduler]
  private implicit val ec: ExecutionContext = StrandEc().ec

  val cancellable: Cancellable                                 = mock[Cancellable]
  val timeServiceScheduler: TimeServiceScheduler               = mock[TimeServiceScheduler]
  val timeServiceSchedulerFactory: TimeServiceSchedulerFactory = mock[TimeServiceSchedulerFactory]
  val task: Unit                                               = {}

  "ScheduleOnce" must {
    "delegate to schedule tasks for given start time" in {
      val startTime = UTCTime.now()

      when(timeServiceSchedulerFactory.make()).thenReturn(timeServiceScheduler)
      when(timeServiceScheduler.scheduleOnce(startTime)(task)).thenReturn(cancellable)

      val actualCancellable = scheduleOnce(startTime)(task)

      verify(timeServiceScheduler).scheduleOnce(startTime)(task)
      actualCancellable shouldBe cancellable
    }
  }

  "schedulePeriodically" must {
    "delegate to schedule task for given interval with default time" in {
      val task: Unit               = {}
      val interval: FiniteDuration = 5.seconds

      when(timeServiceSchedulerFactory.make()).thenReturn(timeServiceScheduler)

      schedulePeriodically(interval)(task)

      verify(timeServiceScheduler).schedulePeriodically(any[TMTTime], argsEq(Duration.ofNanos(interval.toNanos)))(argsEq(task))
    }

    "delegate to schedule task for given interval for given time" in {
      val task: Unit               = {}
      val interval: FiniteDuration = 5.seconds
      val startTime                = UTCTime.now()

      when(timeServiceSchedulerFactory.make()).thenReturn(timeServiceScheduler)

      schedulePeriodically(interval, startTime)(task)

      verify(timeServiceScheduler).schedulePeriodically(argsEq(startTime), argsEq(Duration.ofNanos(interval.toNanos)))(
        argsEq(task)
      )
    }
  }
}

package esw.ocs.dsl.highlevel

import csw.time.core.models.TAITime
import csw.time.core.models.TMTTime
import csw.time.core.models.UTCTime
import csw.time.scheduler.api.Cancellable
import csw.time.scheduler.api.TimeServiceScheduler
import esw.ocs.dsl.SuspendableCallback
import esw.ocs.dsl.jdk.SuspendToJavaConverter
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.nanoseconds
import kotlin.time.toJavaDuration

interface TimeServiceDsl : SuspendToJavaConverter {
    val timeService: TimeServiceScheduler

    fun scheduleOnce(startTime: TMTTime, task: SuspendableCallback): Cancellable =
            timeService.scheduleOnce(startTime, Runnable { task.toJava() })

    fun scheduleOnceFromNow(durationFromNow: Duration, task: SuspendableCallback): Cancellable =
            scheduleOnce(utcTimeAfter(durationFromNow), task)

    fun schedulePeriodically(startTime: TMTTime, interval: Duration, task: SuspendableCallback): Cancellable =
            timeService.schedulePeriodically(
                    startTime,
                    interval.toJavaDuration(),
                    Runnable { task.toJava() })

    fun schedulePeriodicallyFromNow(durationFromNow: Duration, interval: Duration, task: SuspendableCallback): Cancellable =
            schedulePeriodically(utcTimeAfter(durationFromNow), interval, task)

    fun utcTimeNow(): UTCTime = UTCTime.now()

    fun taiTimeNow(): TAITime = TAITime.now()

    fun utcTimeAfter(duration: Duration): UTCTime =
            UTCTime.after(FiniteDuration(duration.toLongNanoseconds(), TimeUnit.NANOSECONDS))

    fun taiTimeAfter(duration: Duration): TAITime =
            TAITime.after(FiniteDuration(duration.toLongNanoseconds(), TimeUnit.NANOSECONDS))

    fun TMTTime.offsetFromNow(): Duration = durationFromNow().toNanos().nanoseconds

}

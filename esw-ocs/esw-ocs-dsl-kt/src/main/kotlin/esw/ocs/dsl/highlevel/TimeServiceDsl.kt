package esw.ocs.dsl.highlevel

import csw.time.core.models.TAITime
import csw.time.core.models.TMTTime
import csw.time.core.models.UTCTime
import csw.time.scheduler.api.Cancellable
import csw.time.scheduler.api.TimeServiceScheduler
import esw.ocs.dsl.jdk.SuspendToJavaConverter
import kotlinx.coroutines.CoroutineScope
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.nanoseconds
import kotlin.time.toJavaDuration

interface TimeServiceDsl : SuspendToJavaConverter {
    val timeServiceScheduler: TimeServiceScheduler

    fun scheduleOnce(startTime: TMTTime, task: suspend CoroutineScope.() -> Unit): Cancellable =
            timeServiceScheduler.scheduleOnce(startTime, Runnable { task.toJava() })

    fun schedulePeriodically(startTime: TMTTime, interval: Duration, task: suspend CoroutineScope.() -> Unit): Cancellable =
            timeServiceScheduler.schedulePeriodically(
                    startTime,
                    interval.toJavaDuration(),
                    Runnable { task.toJava() })

    fun utcTimeNow(): UTCTime = UTCTime.now()

    fun taiTimeNow(): TAITime = TAITime.now()

    fun utcTimeAfter(duration: Duration): UTCTime =
            UTCTime.after(FiniteDuration(duration.toLongNanoseconds(), TimeUnit.NANOSECONDS))

    fun taiTimeAfter(duration: Duration): TAITime =
            TAITime.after(FiniteDuration(duration.toLongNanoseconds(), TimeUnit.NANOSECONDS))

    fun TMTTime.offsetFromNow(): Duration = durationFromNow().toNanos().nanoseconds

}

package esw.ocs.dsl.highlevel

import csw.time.core.models.TAITime
import csw.time.core.models.TMTTime
import csw.time.core.models.UTCTime
import csw.time.scheduler.api.Cancellable
import csw.time.scheduler.api.TimeServiceScheduler
import java.util.concurrent.CompletionStage
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.nanoseconds
import kotlin.time.toJavaDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.future
import scala.concurrent.duration.FiniteDuration

interface TimeServiceDsl {
    val coroutineScope: CoroutineScope

    val timeServiceScheduler: TimeServiceScheduler

    suspend fun scheduleOnce(startTime: TMTTime, task: suspend () -> Unit): Cancellable =
        timeServiceScheduler.scheduleOnce(startTime, Runnable { task.toJavaFuture() })

    fun schedulePeriodically(
        startTime: TMTTime,
        interval: Duration,
        task: suspend () -> Unit
    ): Cancellable =
        timeServiceScheduler.schedulePeriodically(
            startTime,
            interval.toJavaDuration(),
            Runnable { task.toJavaFuture() })

    fun utcTimeNow(): UTCTime = UTCTime.now()

    fun taiTimeNow(): TAITime = TAITime.now()

    fun utcTimeAfter(duration: Duration): UTCTime =
        UTCTime.after(FiniteDuration(duration.toLongNanoseconds(), TimeUnit.NANOSECONDS))

    fun taiTimeAfter(duration: Duration): TAITime =
        TAITime.after(FiniteDuration(duration.toLongNanoseconds(), TimeUnit.NANOSECONDS))

    // fixme : move to appropriate place in clubed extension methods
    fun TMTTime.offsetFromNow(): Duration = durationFromNow().toNanos().nanoseconds

    private fun (suspend () -> Unit).toJavaFuture(): CompletionStage<Void> =
            coroutineScope.future { this@toJavaFuture }.thenAccept { }
}

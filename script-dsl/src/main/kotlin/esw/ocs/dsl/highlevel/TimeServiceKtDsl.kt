package esw.ocs.dsl.highlevel

import csw.time.core.models.TMTTime
import csw.time.scheduler.api.Cancellable
import csw.time.scheduler.api.TimeServiceScheduler
import java.util.concurrent.CompletionStage
import kotlin.time.Duration
import kotlin.time.toJavaDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.future

interface TimeServiceKtDsl : CoroutineScope {
    val timeServiceScheduler: TimeServiceScheduler

    private fun (suspend () -> Unit).toJavaFuture(): CompletionStage<Void> =
        this.let {
            future { it() }.thenAccept { }
        }

    // todo : Verify task works fine this way
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
}

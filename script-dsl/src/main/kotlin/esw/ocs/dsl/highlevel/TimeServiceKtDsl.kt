package esw.ocs.dsl.highlevel

import csw.time.core.models.TMTTime
import csw.time.scheduler.api.Cancellable
import esw.highlevel.dsl.javadsl.Callback
import esw.ocs.impl.dsl.CswServices
import esw.ocs.macros.StrandEc
import java.util.concurrent.CompletionStage
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.toJavaDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.future

interface TimeServiceKtDsl : CoroutineScope {
    val cswServices: CswServices
    fun strandEc(): StrandEc

    private fun (suspend () -> Unit).toJavaFuture(): CompletionStage<Void> =
        this.let {
            return future {
                it()
            }.thenAccept { }
        }

    // todo : Verify task works fine this way
    suspend fun scheduleOnce(startTime: TMTTime, task: suspend () -> Unit): Cancellable =
        cswServices.scheduleOnce(startTime, Callback { task.toJavaFuture() }, strandEc().ec())

    @ExperimentalTime
    fun schedulePeriodically(
        startTime: TMTTime,
        interval: Duration,
        task: suspend () -> Unit
    ): Cancellable =
        cswServices.schedulePeriodically(
            startTime, interval.toJavaDuration(), Callback { task.toJavaFuture() }, strandEc().ec()
        )
}

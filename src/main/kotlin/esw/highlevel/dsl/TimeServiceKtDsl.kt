package esw.highlevel.dsl

import csw.time.core.models.TMTTime
import csw.time.scheduler.api.Cancellable
import esw.ocs.dsl.CswServices
import esw.ocs.macros.StrandEc
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.toJavaDuration

interface TimeServiceKtDsl : CoroutineScope {
    val cswServices: CswServices
    fun strandEc(): StrandEc

    fun scheduleOnce(startTime: TMTTime, task: () -> Unit): Cancellable =
        cswServices.scheduleOnce(startTime, task, strandEc().ec())

    @ExperimentalTime
    fun schedulePeriodically(
        startTime: TMTTime,
        interval: Duration,
        task: () -> Unit
    ): Cancellable = cswServices.schedulePeriodically(startTime, interval.toJavaDuration(), task, strandEc().ec())

}
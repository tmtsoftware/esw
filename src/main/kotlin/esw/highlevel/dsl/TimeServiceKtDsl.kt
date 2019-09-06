package esw.highlevel.dsl

import csw.time.core.models.TMTTime
import csw.time.scheduler.api.Cancellable
import esw.ocs.dsl.CswServices
import esw.ocs.macros.StrandEc
import kotlinx.coroutines.CoroutineScope
import java.time.Duration

interface TimeServiceKtDsl : CoroutineScope {
    val cswServices: CswServices
    fun strandEc(): StrandEc

    fun scheduleOnce0(startTime: TMTTime, task: Runnable): Cancellable =
        cswServices.scheduleOnce(startTime, task, strandEc().ec())

    fun schedulePeriodically0(
        startTime: TMTTime,
        interval: Duration,
        task: () -> Unit
    ): Cancellable = cswServices.schedulePeriodically(startTime, interval, task, strandEc().ec())

}
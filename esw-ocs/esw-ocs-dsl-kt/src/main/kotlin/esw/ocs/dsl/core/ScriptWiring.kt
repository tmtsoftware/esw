package esw.ocs.dsl.core

import esw.ocs.dsl.script.StrandEc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher

internal class ScriptWiring {
    val strandEc: StrandEc by lazy { StrandEc.apply() }
    private val supervisorJob by lazy { SupervisorJob() }
    private val dispatcher by lazy { strandEc.executorService().asCoroutineDispatcher() }
    val scope: CoroutineScope by lazy { CoroutineScope(supervisorJob + dispatcher) }

    fun shutdown() {
        supervisorJob.cancel()
        dispatcher.close()
    }
}
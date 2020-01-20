package esw.ocs.dsl.internal

import esw.ocs.dsl.lowlevel.CswServices
import esw.ocs.dsl.script.StrandEc
import esw.ocs.impl.script.ScriptContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher

class ScriptWiring(val scriptContext: ScriptContext) {
    val strandEc: StrandEc by lazy { StrandEc.apply() }
    private val supervisorJob by lazy { SupervisorJob() }
    private val dispatcher by lazy { strandEc.executorService().asCoroutineDispatcher() }
    val scope: CoroutineScope by lazy { CoroutineScope(supervisorJob + dispatcher) }
    val cswServices: CswServices by lazy { CswServices.create(scriptContext, strandEc) }

    //todo: this is not being called from anywhere
    fun shutdown() {
        supervisorJob.cancel()
        dispatcher.close()
    }
}
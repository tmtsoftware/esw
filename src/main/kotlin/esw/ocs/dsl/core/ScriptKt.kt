package esw.ocs.dsl.core

import csw.params.commands.Setup
import esw.highlevel.dsl.CswHighLevelDsl
import esw.ocs.dsl.CswServices
import esw.ocs.dsl.javadsl.JScript
import esw.ocs.macros.StrandEc
import kotlinx.coroutines.*
import kotlinx.coroutines.future.future
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

sealed class BaseScript(override val cswServices: CswServices) : JScript(cswServices), CswHighLevelDsl, CoroutineScope {

    fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")

    fun handleSetup(name: String, block: suspend (Setup) -> Unit) {
        jHandleSetupCommand(name) { setup: Setup ->
            future {
                block(setup)
            }.thenAccept { }
        }
    }

    fun handleShutdown(block: suspend () -> Unit) {
        jHandleShutdown {
            future {
                block()
            }.thenAccept { }
        }
    }

    fun <T> CoroutineScope.par(block: suspend CoroutineScope.() -> T): Deferred<T> = async { block() }

    fun loadScripts(vararg reusableScriptResult: ReusableScriptResult) {
        reusableScriptResult.forEach {
            this.merge(it(cswServices, strandEc(), coroutineContext))
        }
    }
}

class ReusableScript(
    cswServices: CswServices,
    private val _strandEc: StrandEc,
    override val coroutineContext: CoroutineContext
) : BaseScript(cswServices) {

    override fun strandEc(): StrandEc = _strandEc

}

open class ScriptKt(cswServices: CswServices) : BaseScript(cswServices) {

    private val job = Job()
    private val ec = Executors.newSingleThreadScheduledExecutor()
    private val dispatcher = ec.asCoroutineDispatcher()
    private val _strandEc = StrandEc(ec)

    override fun strandEc(): StrandEc = _strandEc

    override val coroutineContext: CoroutineContext
        get() = job + dispatcher

    fun close() {
        job.cancel()
        dispatcher.close()
    }
}
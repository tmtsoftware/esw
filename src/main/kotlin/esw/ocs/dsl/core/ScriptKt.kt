package esw.ocs.dsl.core

import csw.params.commands.Observe
import csw.params.commands.Setup
import esw.highlevel.dsl.CswHighLevelDsl
import esw.ocs.dsl.CswServices
import esw.ocs.dsl.StopIf
import esw.ocs.dsl.javadsl.JScript
import esw.ocs.macros.StrandEc
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletionStage
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.toJavaDuration

sealed class BaseScript(override val cswServices: CswServices) : JScript(cswServices), CswHighLevelDsl, CoroutineScope {

    fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")

    fun handleSetup(name: String, block: suspend (Setup) -> Unit) {
        jHandleSetupCommand(name) { block.toJavaFuture(it) }
    }

    fun handleObserve(name: String, block: suspend (Observe) -> Unit) {
        jHandleObserveCommand(name) { block.toJavaFuture(it) }
    }

    fun handleGoOnline(block: suspend () -> Unit) {
        jHandleGoOnline { block.toJavaFutureVoid() }
    }

    fun handleGoOffline(block: suspend () -> Unit) {
        jHandleGoOffline { block.toJavaFutureVoid() }
    }

    fun handleAbort(block: suspend () -> Unit) {
        jHandleAbort { block.toJavaFutureVoid() }
    }

    fun handleShutdown(block: suspend () -> Unit) {
        jHandleShutdown { block.toJavaFutureVoid() }
    }

    fun <T> CoroutineScope.par(block: suspend CoroutineScope.() -> T): Deferred<T> = async { block() }

    @ExperimentalTime
    suspend fun loop(duration: Duration, block: suspend () -> StopIf) {
        jLoop(duration.toJavaDuration()) { block.toJavaFuture() }.await()
    }

    fun loadScripts(vararg reusableScriptResult: ReusableScriptResult) {
        reusableScriptResult.forEach {
            this.merge(it(cswServices, strandEc(), coroutineContext))
        }
    }

    private fun <T> (suspend () -> T).toJavaFuture(): CompletionStage<T> =
        this.let {
            return future { it() }
        }


    private fun (suspend () -> Unit).toJavaFutureVoid(): CompletionStage<Void> =
        this.let {
            return future {
                it()
            }.thenAccept { }
        }


    private fun <T> (suspend (T) -> Unit).toJavaFuture(value: T): CompletionStage<Void> =
        this.let {
            return future {
                it(value)
            }.thenAccept { }
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
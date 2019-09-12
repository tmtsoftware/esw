package esw.ocs.dsl.core

import csw.params.commands.Observe
import csw.params.commands.Sequence
import csw.params.commands.SequenceCommand
import csw.params.commands.Setup
import esw.ocs.dsl.highlevel.CswHighLevelDsl
import esw.ocs.dsl.nullable
import esw.ocs.impl.dsl.CswServices
import esw.ocs.impl.dsl.StopIf
import esw.ocs.impl.dsl.javadsl.JScript
import esw.ocs.macros.StrandEc
import java.util.concurrent.CompletionStage
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.toJavaDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future

sealed class BaseScript : CoroutineScope, CswHighLevelDsl {

    // this needs to be lazy otherwise handlers does not get loaded properly
    val jScript: JScript by lazy { JScriptFactory.make(cswServices, strandEc()) }

    suspend fun nextIf(predicate: (SequenceCommand) -> Boolean): SequenceCommand? =
        jScript.jNextIf { predicate(it) }.await().nullable()

    fun handleSetup(name: String, block: suspend (Setup) -> Unit) {
        jScript.jHandleSetupCommand(name) { block.toJavaFuture(it) }
    }

    fun handleObserve(name: String, block: suspend (Observe) -> Unit) {
        jScript.jHandleObserveCommand(name) { block.toJavaFuture(it) }
    }

    fun handleGoOnline(block: suspend () -> Unit) {
        jScript.jHandleGoOnline { block.toJavaFutureVoid() }
    }

    fun handleGoOffline(block: suspend () -> Unit) {
        jScript.jHandleGoOffline { block.toJavaFutureVoid() }
    }

    fun handleAbort(block: suspend () -> Unit) {
        jScript.jHandleAbort { block.toJavaFutureVoid() }
    }

    fun handleShutdown(block: suspend () -> Unit) {
        jScript.jHandleShutdown { block.toJavaFutureVoid() }
    }

    @ExperimentalTime
    suspend fun loop(duration: Duration, block: suspend () -> StopIf) {
        jScript.jLoop(duration.toJavaDuration()) { block.toJavaFuture() }.await()
    }

    fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")

    fun loadScripts(vararg reusableScriptResult: ReusableScriptResult) {
        reusableScriptResult.forEach {
            this.jScript.merge(it(cswServices, strandEc(), coroutineContext).jScript)
        }
    }

    // fixme: return SubmitResponse
    suspend fun submitSequence(sequencerName: String, observingMode: String, sequence: Sequence) {
        val location = resolveSequencer(sequencerName, observingMode)
        cswServices.submitSequence(location, sequence)
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
    override val cswServices: CswServices,
    private val _strandEc: StrandEc,
    override val coroutineContext: CoroutineContext
) : BaseScript() {
    override fun strandEc(): StrandEc = _strandEc
}

open class ScriptKt(override val cswServices: CswServices) : BaseScript() {
    private val ec = Executors.newSingleThreadScheduledExecutor()
    private val _strandEc = StrandEc(ec)
    private val job = Job()
    private val dispatcher = ec.asCoroutineDispatcher()

    override val coroutineContext: CoroutineContext
        get() = job + dispatcher

    override fun strandEc(): StrandEc = _strandEc

    fun close() {
        job.cancel()
        dispatcher.close()
    }
}

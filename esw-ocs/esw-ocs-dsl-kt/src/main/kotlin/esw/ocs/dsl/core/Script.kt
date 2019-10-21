package esw.ocs.dsl.core

import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Observe
import csw.params.commands.Sequence
import csw.params.commands.SequenceCommand
import csw.params.commands.Setup
import csw.time.core.models.UTCTime
import esw.ocs.dsl.highlevel.CswHighLevelDsl
import esw.ocs.dsl.nullable
import esw.ocs.dsl.script.CswServices
import esw.ocs.dsl.script.JScriptDsl
import esw.ocs.dsl.script.StrandEc
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

sealed class ScriptDslKt(private val cswServices: CswServices) : CswHighLevelDsl(cswServices) {

    // this needs to be lazy otherwise handlers does not get loaded properly
    val scriptDsl: JScriptDsl by lazy { ScriptDslFactory.make(cswServices, strandEc) }

    fun initialize(block: suspend CoroutineScope.() -> Unit) = scriptDsl.addInitializer { runBlocking { block(); null } }

    suspend fun nextIf(predicate: (SequenceCommand) -> Boolean): SequenceCommand? =
        scriptDsl.nextIf { predicate(it) }.await().nullable()

    fun handleSetup(name: String, block: suspend CoroutineScope.(Setup) -> Unit) {
        scriptDsl.handleSetupCommand(name) { block.toJavaFuture(it) }
    }

    fun handleObserve(name: String, block: suspend CoroutineScope.(Observe) -> Unit) {
        scriptDsl.handleObserveCommand(name) { block.toJavaFuture(it) }
    }

    fun handleGoOnline(block: suspend CoroutineScope.() -> Unit) {
        scriptDsl.handleGoOnline { block.toJavaFutureVoid() }
    }

    fun handleGoOffline(block: suspend CoroutineScope.() -> Unit) {
        scriptDsl.handleGoOffline { block.toJavaFutureVoid() }
    }

    fun handleAbortSequence(block: suspend CoroutineScope.() -> Unit) {
        scriptDsl.handleAbortSequence { block.toJavaFutureVoid() }
    }

    fun handleShutdown(block: suspend CoroutineScope.() -> Unit) {
        scriptDsl.handleShutdown { block.toJavaFutureVoid() }
    }

    fun handleDiagnosticMode(block: suspend (UTCTime, String) -> Unit) {
        scriptDsl.handleDiagnosticMode { x: UTCTime, y: String -> coroutineScope.future { block(x, y) }.thenAccept { } }
    }

    fun handleOperationsMode(block: suspend CoroutineScope.() -> Unit) {
        scriptDsl.handleOperationsMode { block.toJavaFutureVoid() }
    }

    fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")

    fun loadScripts(vararg reusableScriptResult: ReusableScriptResult) {
        reusableScriptResult.forEach {
            this.scriptDsl.merge(it(cswServices, strandEc, coroutineScope).scriptDsl)
        }
    }

    suspend fun submitSequence(sequencerName: String, observingMode: String, sequence: Sequence): SubmitResponse =
        this.scriptDsl.submitSequence(sequencerName, observingMode, sequence).await()

    private fun (suspend CoroutineScope.() -> Unit).toJavaFutureVoid(): CompletionStage<Void> =
        coroutineScope.future { this@toJavaFutureVoid() }
            .whenComplete { v, e ->
                if (e == null) {
                    CompletableFuture.completedFuture(v)
                } else {
                    log("exception : ${e.message}")
                    // fixme: call exception handlers whenever implemented
                    CompletableFuture.failedFuture<Unit>(e)
                }
            }
            .thenAccept { }

    private fun <T> (suspend CoroutineScope.(T) -> Unit).toJavaFuture(value: T): CompletionStage<Void> {
        val curriedBlock: suspend (CoroutineScope) -> Unit = { a: CoroutineScope -> this(a, value) }
        return curriedBlock.toJavaFutureVoid()
    }
}

class ReusableScript(
    cswServices: CswServices,
    override val strandEc: StrandEc,
    override val coroutineScope: CoroutineScope
) : ScriptDslKt(cswServices)


open class Script(cswServices: CswServices) : ScriptDslKt(cswServices) {
    private val _strandEc = StrandEc.apply()
    private val supervisorJob = SupervisorJob()
    private val dispatcher = _strandEc.executorService().asCoroutineDispatcher()

    private val exceptionHandler = CoroutineExceptionHandler {
        // fixme: call exception handlers whenever implemented
        _, exception ->
        log("Exception: ${exception.message}")
    }
    override val coroutineScope: CoroutineScope get() = CoroutineScope(supervisorJob + dispatcher + exceptionHandler)
    override val strandEc: StrandEc get() = _strandEc

    fun close() {
        supervisorJob.cancel()
        dispatcher.close()
    }
}

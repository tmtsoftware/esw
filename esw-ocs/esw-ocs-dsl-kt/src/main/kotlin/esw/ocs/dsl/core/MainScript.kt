package esw.ocs.dsl.core

import csw.params.commands.Observe
import csw.params.commands.SequenceCommand
import csw.params.commands.Setup
import csw.time.core.models.UTCTime
import esw.ocs.dsl.highlevel.CswHighLevelDsl
import esw.ocs.dsl.nullable
import esw.ocs.dsl.script.CswServices
import esw.ocs.dsl.script.FSMScriptDsl
import esw.ocs.dsl.script.ScriptDsl
import esw.ocs.dsl.script.StrandEc
import esw.ocs.dsl.script.exceptions.ScriptLoadingException.ScriptInitialisationFailedException
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future

sealed class CommonScriptDsl(val cswServices: CswServices) : CswHighLevelDsl(cswServices) {
    internal open val scriptDsl: ScriptDsl by lazy { ScriptDsl(cswServices, strandEc) }

    suspend fun nextIf(predicate: (SequenceCommand) -> Boolean): SequenceCommand? =
            scriptDsl.nextIf { predicate(it) }.await().nullable()

    fun finishWithError(message: String = ""): Nothing = throw RuntimeException(message)

    fun onSetup(name: String, block: suspend CoroutineScope.(Setup) -> Unit) =
            scriptDsl.onSetupCommand(name) { block.toJava(it) }

    fun onObserve(name: String, block: suspend CoroutineScope.(Observe) -> Unit) =
            scriptDsl.onObserveCommand(name) { block.toJava(it) }

    fun onGoOnline(block: suspend CoroutineScope.() -> Unit) =
            scriptDsl.onGoOnline { block.toJava() }

    fun onGoOffline(block: suspend CoroutineScope.() -> Unit) =
            scriptDsl.onGoOffline { block.toJava() }

    fun onAbortSequence(block: suspend CoroutineScope.() -> Unit) =
            scriptDsl.onAbortSequence { block.toJava() }

    fun onShutdown(block: suspend CoroutineScope.() -> Unit) =
            scriptDsl.onShutdown { block.toJava() }

    fun onDiagnosticMode(block: suspend (UTCTime, String) -> Unit) =
            scriptDsl.onDiagnosticMode { x: UTCTime, y: String ->
                coroutineScope.launch { block(x, y) }.asCompletableFuture().thenAccept { }
            }

    fun onOperationsMode(block: suspend CoroutineScope.() -> Unit) =
            scriptDsl.onOperationsMode { block.toJava() }

    fun onStop(block: suspend CoroutineScope.() -> Unit) =
            scriptDsl.onStop { block.toJava() }

    fun onException(block: suspend CoroutineScope.(Throwable) -> Unit) =
            scriptDsl.onException {
                // "future" is used to swallow the exception coming from exception handlers
                coroutineScope.future { block(it) }
                        .exceptionally { error("Exception thrown from Exception handler with a message : ${it.message}", ex = it) }
                        .thenAccept { }
            }

    fun loadScripts(vararg reusableScriptResult: ReusableScriptResult) =
            reusableScriptResult.forEach {
                this.scriptDsl.merge(it(cswServices, strandEc, coroutineScope).scriptDsl)
            }
}

class ReusableScript(
        cswServices: CswServices,
        override val strandEc: StrandEc,
        override val coroutineScope: CoroutineScope
) : CommonScriptDsl(cswServices)

open class MainScript(cswServices: CswServices) : CommonScriptDsl(cswServices) {
    private val _strandEc = StrandEc.apply()
    private val supervisorJob = SupervisorJob()
    private val dispatcher = _strandEc.executorService().asCoroutineDispatcher()

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        warn("Exception thrown in script with a message: ${exception.message}, invoking exception handler", ex = exception)
        scriptDsl.executeExceptionHandlers(exception)
    }

    override val coroutineScope: CoroutineScope get() = CoroutineScope(supervisorJob + dispatcher + exceptionHandler)
    override val strandEc: StrandEc get() = _strandEc

    // fixme: call me when shutting down sequencer
    fun close() {
        supervisorJob.cancel()
        dispatcher.close()
    }
}

class FSMScript(cswServices: CswServices) : MainScript(cswServices) {
    private val fsmScriptDsl: FSMScriptDsl by lazy { FSMScriptDsl(cswServices, strandEc) }
    override val scriptDsl: ScriptDsl by lazy { fsmScriptDsl }

    fun state(state: String, block: suspend ReusableScript.() -> Unit) {
        fun reusableScript(): ReusableScript = ReusableScript(cswServices, strandEc, coroutineScope).apply {
            try {
                runBlocking { block() }
            } catch (ex: Exception) {
                error("Failed to initialize state: $state", ex = ex)
                throw ScriptInitialisationFailedException(ex.message)
            }
        }

        fsmScriptDsl.add(state) { reusableScript().scriptDsl }
    }
}

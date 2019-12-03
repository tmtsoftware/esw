package esw.ocs.dsl.core

import csw.params.commands.Observe
import csw.params.commands.SequenceCommand
import csw.params.commands.Setup
import csw.time.core.models.UTCTime
import esw.ocs.dsl.ScriptMarker
import esw.ocs.dsl.SuspendableCallback
import esw.ocs.dsl.SuspendableConsumer
import esw.ocs.dsl.highlevel.CswHighLevelDsl
import esw.ocs.dsl.nullable
import esw.ocs.dsl.params.Params
import esw.ocs.dsl.script.CswServices
import esw.ocs.dsl.script.FSMScriptDsl
import esw.ocs.dsl.script.ScriptDsl
import esw.ocs.dsl.script.StrandEc
import esw.ocs.dsl.script.exceptions.ScriptLoadingException.ScriptInitialisationFailedException
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future

sealed class BaseScript(val cswServices: CswServices, scope: CoroutineScope) : CswHighLevelDsl(cswServices) {
    internal open val scriptDsl: ScriptDsl by lazy { ScriptDsl(cswServices, strandEc) }

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        warn("Exception thrown in script with a message: ${exception.message}, invoking exception handler", ex = exception)
        exception.printStackTrace()
        scriptDsl.executeExceptionHandlers(exception)
    }

    override val coroutineScope: CoroutineScope = scope + exceptionHandler

    fun onGoOnline(block: SuspendableCallback) =
            scriptDsl.onGoOnline { block.toJava() }

    fun onGoOffline(block: SuspendableCallback) =
            scriptDsl.onGoOffline { block.toJava() }

    fun onAbortSequence(block: SuspendableCallback) =
            scriptDsl.onAbortSequence { block.toJava() }

    fun onShutdown(block: SuspendableCallback) =
            scriptDsl.onShutdown { block.toJava() }

    fun onDiagnosticMode(block: suspend (UTCTime, String) -> Unit) =
            scriptDsl.onDiagnosticMode { x: UTCTime, y: String ->
                coroutineScope.launch { block(x, y) }.asCompletableFuture().thenAccept { }
            }

    fun onOperationsMode(block: SuspendableCallback) =
            scriptDsl.onOperationsMode { block.toJava() }

    fun onStop(block: SuspendableCallback) =
            scriptDsl.onStop { block.toJava() }

}

@ScriptMarker
open class Script(
        cswServices: CswServices,
        override val strandEc: StrandEc,
        scope: CoroutineScope
) : BaseScript(cswServices, scope) {

    fun finishWithError(message: String = ""): Nothing = throw RuntimeException(message)

    suspend fun nextIf(predicate: (SequenceCommand) -> Boolean): SequenceCommand? =
            scriptDsl.nextIf { predicate(it) }.await().nullable()

    fun onSetup(name: String, block: SuspendableConsumer<Setup>): CommandHandlerKt<Setup> {
        val handler = CommandHandlerKt(block, coroutineScope)
        scriptDsl.onSetupCommand(name, handler)
        return handler
    }

    fun onObserve(name: String, block: SuspendableConsumer<Observe>): CommandHandlerKt<Observe> {
        val handler = CommandHandlerKt(block, coroutineScope)
        scriptDsl.onObserveCommand(name, handler)
        return handler
    }

    fun onException(block: SuspendableConsumer<Throwable>) =
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

class FSMStateDsl(
        cswServices: CswServices,
        override val strandEc: StrandEc,
        scope: CoroutineScope,
        val fsmScriptDsl: FSMScriptDsl
) : Script(cswServices, strandEc, scope) {
    fun become(nextState: String, params: Params = Params(setOf())) = fsmScriptDsl.become(nextState, params)
}

@ScriptMarker
class FSMScript(
        cswServices: CswServices,
        override val strandEc: StrandEc,
        scope: CoroutineScope
) : BaseScript(cswServices, scope) {
    internal val fsmScriptDsl: FSMScriptDsl by lazy { FSMScriptDsl(cswServices, strandEc) }

    override val scriptDsl: ScriptDsl by lazy { fsmScriptDsl }

    fun state(state: String, block: suspend FSMStateDsl.(Params) -> Unit) {
        fun reusableScript(): FSMStateDsl = FSMStateDsl(cswServices, strandEc, coroutineScope, fsmScriptDsl).apply {
            try {
                runBlocking { block(fsmScriptDsl.state.params()) }
            } catch (ex: Exception) {
                error("Failed to initialize state: $state", ex = ex)
                throw ScriptInitialisationFailedException(ex.message)
            }
        }

        fsmScriptDsl.add(state) { reusableScript().scriptDsl }
    }

    internal fun become(nextState: String, params: Params = Params(setOf())) = fsmScriptDsl.become(nextState, params)
}

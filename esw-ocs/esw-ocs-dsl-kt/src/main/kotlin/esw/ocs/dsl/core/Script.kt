package esw.ocs.dsl.core

import csw.params.commands.Observe
import csw.params.commands.SequenceCommand
import csw.params.commands.Setup
import csw.time.core.models.UTCTime
import esw.ocs.dsl.highlevel.CswHighLevelDsl
import esw.ocs.dsl.highlevel.models.ScriptError
import esw.ocs.dsl.internal.ScriptWiring
import esw.ocs.dsl.nullable
import esw.ocs.dsl.params.Params
import esw.ocs.dsl.script.FsmScriptDsl
import esw.ocs.dsl.script.ScriptDsl
import esw.ocs.dsl.script.StrandEc
import esw.ocs.dsl.script.exceptions.ScriptLoadingException.ScriptInitialisationFailedException
import esw.ocs.dsl.toScriptError
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import kotlin.coroutines.CoroutineContext

sealed class BaseScript(wiring: ScriptWiring) : CswHighLevelDsl(wiring.cswServices, wiring.scriptContext), HandlerScope {
    internal open val scriptDsl: ScriptDsl by lazy { ScriptDsl(wiring.scriptContext.sequenceOperatorFactory(), strandEc) }

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        warn("Exception thrown in script with a message: ${exception.message}, invoking exception handler", ex = exception)
        exception.printStackTrace()
        scriptDsl.executeExceptionHandlers(exception)
    }

    override val coroutineScope: CoroutineScope = wiring.scope + exceptionHandler

    fun onGoOnline(block: suspend HandlerScope.() -> Unit) =
            scriptDsl.onGoOnline { block.toCoroutineScope().toJava() }

    fun onGoOffline(block: suspend HandlerScope.() -> Unit) =
            scriptDsl.onGoOffline { block.toCoroutineScope().toJava() }

    fun onAbortSequence(block: suspend HandlerScope.() -> Unit) =
            scriptDsl.onAbortSequence { block.toCoroutineScope().toJava() }

    fun onShutdown(block: suspend HandlerScope.() -> Unit) =
            scriptDsl.onShutdown { block.toCoroutineScope().toJava() }

    fun onDiagnosticMode(block: suspend HandlerScope.(UTCTime, String) -> Unit) =
            scriptDsl.onDiagnosticMode { x: UTCTime, y: String ->
                coroutineScope.launch { block(this.toHandlerScope(), x, y) }.asCompletableFuture().thenAccept { }
            }

    fun onOperationsMode(block: suspend HandlerScope.() -> Unit) =
            scriptDsl.onOperationsMode { block.toCoroutineScope().toJava() }

    fun onStop(block: suspend HandlerScope.() -> Unit) =
            scriptDsl.onStop { block.toCoroutineScope().toJava() }

    internal fun CoroutineScope.toHandlerScope(): HandlerScope = object : HandlerScope by this@BaseScript {
        override val coroutineContext: CoroutineContext = this@toHandlerScope.coroutineContext
    }

    private fun (suspend HandlerScope.() -> Unit).toCoroutineScope(): suspend (CoroutineScope) -> Unit = { this(it.toHandlerScope()) }
}

open class Script(private val wiring: ScriptWiring) : BaseScript(wiring), ScriptScope, CommandHandlerScope {
    override val strandEc: StrandEc = wiring.strandEc

    //todo : revisit all the places implementing CoroutineContext
    override val coroutineContext: CoroutineContext = wiring.scope.coroutineContext // this won't be used anywhere

    override suspend fun nextIf(predicate: (SequenceCommand) -> Boolean): SequenceCommand? =
            scriptDsl.nextIf { predicate(it) }.await().nullable()

    override fun onSetup(name: String, block: suspend CommandHandlerScope.(Setup) -> Unit): CommandHandlerKt<Setup> {
        val handler = CommandHandlerKt(coroutineScope, this, block.toCoroutineScope())
        scriptDsl.onSetupCommand(name, handler)
        return handler
    }

    override fun onObserve(name: String, block: suspend CommandHandlerScope.(Observe) -> Unit): CommandHandlerKt<Observe> {
        val handler = CommandHandlerKt(coroutineScope, this, block.toCoroutineScope())
        scriptDsl.onObserveCommand(name, handler)
        return handler
    }

    override fun onGlobalError(block: suspend HandlerScope.(ScriptError) -> Unit) =
            scriptDsl.onException {
                // "future" is used to swallow the exception coming from exception handlers
                coroutineScope.future { block(this.toHandlerScope(), it.toScriptError()) }
                        .exceptionally { error("Exception thrown from Exception handler with a message : ${it.message}", ex = it) }
                        .thenAccept { }
            }

    override fun loadScripts(vararg reusableScriptResult: ReusableScriptResult) =
            reusableScriptResult.forEach {
                this.scriptDsl.merge(it(wiring).scriptDsl)
            }

    override fun become(nextState: String, params: Params): Unit = throw RuntimeException("Become can not be called outside Fsm scripts")

    private fun <T> (suspend CommandHandlerScope.(T) -> Unit).toCoroutineScope(): suspend (CoroutineScope, T) -> Unit = { _scope, value ->
        val commandHandlerScope = object : CommandHandlerScope by this@Script {
            override val coroutineContext: CoroutineContext = _scope.coroutineContext
        }
        this.invoke(commandHandlerScope, value)
    }
}

class FsmScript(private val wiring: ScriptWiring) : BaseScript(wiring), FsmScriptScope {

    override val strandEc: StrandEc = wiring.strandEc
    override val coroutineContext: CoroutineContext = coroutineScope.coroutineContext
    override val scriptDsl: ScriptDsl by lazy { fsmScriptDsl }

    internal val fsmScriptDsl: FsmScriptDsl by lazy { FsmScriptDsl(wiring.scriptContext.sequenceOperatorFactory(), strandEc) }

    inner class FsmScriptStateDsl : Script(wiring), FsmScriptStateScope {
        override val coroutineContext: CoroutineContext = this@FsmScript.coroutineScope.coroutineContext
        override fun become(nextState: String, params: Params) = this@FsmScript.become(nextState, params)
    }

    override fun state(name: String, block: suspend FsmScriptStateScope.(Params) -> Unit) {

        fun reusableScript(): FsmScriptStateDsl = FsmScriptStateDsl().apply {
            try {
                runBlocking { block(this@FsmScript.fsmScriptDsl.state.params()) }
            } catch (ex: Exception) {
                error("Failed to initialize state: $name", ex = ex)
                throw ScriptInitialisationFailedException(ex.message)
            }
        }

        fsmScriptDsl.add(name) { reusableScript().scriptDsl }
    }

    override fun become(nextState: String, params: Params) = fsmScriptDsl.become(nextState, params)
}

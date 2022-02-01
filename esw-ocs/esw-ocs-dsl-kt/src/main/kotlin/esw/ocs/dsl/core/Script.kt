package esw.ocs.dsl.core

import akka.actor.typed.ActorSystem
import akka.actor.typed.SpawnProtocol
import csw.params.commands.Observe
import csw.params.commands.SequenceCommand
import csw.params.commands.Setup
import csw.params.events.SequencerObserveEvent
import csw.prefix.models.Prefix
import csw.time.core.models.UTCTime
import esw.ocs.api.models.ObsMode
import esw.ocs.dsl.highlevel.CswHighLevelDsl
import esw.ocs.dsl.highlevel.models.ScriptError
import esw.ocs.dsl.internal.ScriptWiring
import esw.ocs.dsl.nullable
import esw.ocs.dsl.params.Params
import esw.ocs.dsl.script.FsmScriptDsl
import esw.ocs.dsl.script.ScriptDsl
import esw.ocs.dsl.script.StrandEc
import esw.ocs.dsl.script.exceptions.ScriptInitialisationFailedException
import esw.ocs.dsl.shutdownCpuBoundDispatcher
import esw.ocs.dsl.toScriptError
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toKotlinDuration

/**
 * Base Class for all the scripts(sequencer-script, FSM)
 * which contains the implementation of handlers like onSetup, OnObserve, OnNewSequence etc.
 *
 * @constructor
 *
 * @param wiring - An instance of script wiring
 */
sealed class BaseScript(wiring: ScriptWiring) : CswHighLevelDsl(wiring.cswServices, wiring.scriptContext),
        HandlerScope {
    override val actorSystem: ActorSystem<SpawnProtocol.Command> = wiring.scriptContext.actorSystem()
    protected val shutdownTask = Runnable { wiring.shutdown() }
    internal open val scriptDsl: ScriptDsl by lazy {
        ScriptDsl(
                wiring.scriptContext.sequenceOperatorFactory(),
                logger,
                strandEc,
                shutdownTask
        )
    }
    override val isOnline: Boolean get() = scriptDsl.isOnline
    final override val prefix: String = wiring.scriptContext.prefix().toString()
    final override val obsMode: ObsMode = wiring.scriptContext.obsMode()
    override val sequencerObserveEvent: SequencerObserveEvent = SequencerObserveEvent(Prefix.apply(prefix))

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        error("Exception thrown in script with the message: [${exception.message}], invoking exception handler")
        scriptDsl.executeExceptionHandlers(exception)
    }

    private val shutdownExceptionHandler = CoroutineExceptionHandler { _, exception ->
        error("Shutting down: Exception thrown in script with the message: [${exception.message}]")
    }

    override val coroutineScope: CoroutineScope = wiring.scope + exceptionHandler

    private val shutdownHandlerCoroutineScope = wiring.scope + shutdownExceptionHandler

    fun onNewSequence(block: suspend HandlerScope.() -> Unit) =
            scriptDsl.onNewSequence { block.toCoroutineScope().toJava() }

    fun onGoOnline(block: suspend HandlerScope.() -> Unit) =
            scriptDsl.onGoOnline { block.toCoroutineScope().toJava() }

    fun onGoOffline(block: suspend HandlerScope.() -> Unit) =
            scriptDsl.onGoOffline { block.toCoroutineScope().toJava() }

    fun onAbortSequence(block: suspend HandlerScope.() -> Unit) =
            scriptDsl.onAbortSequence { block.toCoroutineScope().toJava() }

    fun onShutdown(block: suspend HandlerScope.() -> Unit) =
            scriptDsl.onShutdown {
                block.toCoroutineScope().toJava(shutdownHandlerCoroutineScope).whenComplete { _, _ ->
                    // cleanup cpu bound dispatcher in case script has used it, as a part of shutdown process
                    shutdownCpuBoundDispatcher()
                }
            }

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

    private fun (suspend HandlerScope.() -> Unit).toCoroutineScope(): suspend (CoroutineScope) -> Unit =
            { this(it.toHandlerScope()) }
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

    override fun onObserve(
            name: String,
            block: suspend CommandHandlerScope.(Observe) -> Unit
    ): CommandHandlerKt<Observe> {
        val handler = CommandHandlerKt(coroutineScope, this, block.toCoroutineScope())
        scriptDsl.onObserveCommand(name, handler)
        return handler
    }

    override fun onGlobalError(block: suspend HandlerScope.(ScriptError) -> Unit) =
            scriptDsl.onException {
                // "future" is used to swallow the exception coming from exception handlers
                coroutineScope.future { block(this.toHandlerScope(), it.toScriptError()) }
                        .exceptionally {
                            error(
                                    "Exception thrown from Exception handler with a message : ${it.message}",
                                    cause = it
                            )
                        }
                        .thenAccept { }
            }

    override fun loadScripts(vararg reusableScriptResult: ReusableScriptResult) =
            reusableScriptResult.forEach {
                this.scriptDsl.merge(it(wiring).scriptDsl)
            }

    override fun become(nextState: String, params: Params): Unit =
            warn("`become` method should not be called from regular script, calling it will do nothing")

    private fun <T> (suspend CommandHandlerScope.(T) -> Unit).toCoroutineScope(): suspend (CoroutineScope, T) -> Unit =
            { _scope, value ->
                val commandHandlerScope = object : CommandHandlerScope by this@Script {
                    override val coroutineContext: CoroutineContext = _scope.coroutineContext
                }
                this.invoke(commandHandlerScope, value)
            }

    fun startHealthCheck() {
        val heartbeatInterval = wiring.heartbeatInterval.toKotlinDuration()
        loopAsync(heartbeatInterval) {
            wiring.heartbeatChannel.send(Unit)
        }

        launch {
            withContext(Dispatchers.Default) {
                loop(heartbeatInterval.plus(10.milliseconds)) {
                    val heartbeat = wiring.heartbeatChannel.tryReceive()
                    if (heartbeat.isSuccess) trace("[StrandEC Heartbeat Received]")
                    if (heartbeat.isClosed) error("[StrandEC Heartbeat Channel Closed] Unable to perform heartbeat check")
                    if (heartbeat.isFailure) error(
                            "[StrandEC Heartbeat Delayed] - Scheduled sending of heartbeat was delayed. " +
                                    "The reason can be thread starvation, e.g. by running blocking tasks in sequencer script, CPU overload, or GC."
                    )
                }
            }
        }
    }
}

class FsmScript(private val wiring: ScriptWiring) : BaseScript(wiring), FsmScriptScope {

    override val strandEc: StrandEc = wiring.strandEc
    override val coroutineContext: CoroutineContext = coroutineScope.coroutineContext
    override val scriptDsl: ScriptDsl by lazy { fsmScriptDsl }

    internal val fsmScriptDsl: FsmScriptDsl by lazy {
        FsmScriptDsl(
                wiring.scriptContext.sequenceOperatorFactory(),
                logger,
                strandEc,
                shutdownTask
        )
    }

    inner class FsmScriptStateDsl : Script(wiring), FsmScriptStateScope {
        override val coroutineContext: CoroutineContext = this@FsmScript.coroutineScope.coroutineContext
        override fun become(nextState: String, params: Params) = this@FsmScript.become(nextState, params)
    }

    override fun state(name: String, block: suspend FsmScriptStateScope.(Params) -> Unit) {

        fun reusableScript(): FsmScriptStateDsl = FsmScriptStateDsl().apply {
            try {
                runBlocking { block(this@FsmScript.fsmScriptDsl.state.params()) }
            } catch (ex: Exception) {
                error("Failed to initialize state: $name", cause = ex)
                throw ScriptInitialisationFailedException(ex.message)
            }
        }

        fsmScriptDsl.add(name) { reusableScript().scriptDsl }
    }

    override fun become(nextState: String, params: Params) = fsmScriptDsl.become(nextState, params)
}

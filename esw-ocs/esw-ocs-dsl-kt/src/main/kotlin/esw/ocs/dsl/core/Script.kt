package esw.ocs.dsl.core

import csw.alarm.api.javadsl.IAlarmService
import csw.config.api.javadsl.IConfigClientService
import csw.event.api.javadsl.IEventService
import csw.location.api.javadsl.ILocationService
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
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future

sealed class ScriptDslKt(val cswServices: CswServices) : CswHighLevelDsl(cswServices) {

    // https://stackoverflow.com/questions/58497383/is-it-possible-to-provide-custom-name-for-internal-delegated-properties-in-kotli/58497535#58497535
    @get:JvmName("scriptDsl")
    internal val scriptDsl: JScriptDsl by lazy { ScriptDslFactory.make(cswServices, strandEc) }

    suspend fun nextIf(predicate: (SequenceCommand) -> Boolean): SequenceCommand? =
            scriptDsl.nextIf { predicate(it) }.await().nullable()

    fun finishWithError(message: String = ""): Nothing = throw RuntimeException(message)

    fun onSetup(name: String, block: suspend CoroutineScope.(Setup) -> Unit) =
            scriptDsl.onSetupCommand(name) { block.toJavaFuture(it) }

    fun onObserve(name: String, block: suspend CoroutineScope.(Observe) -> Unit) =
            scriptDsl.onObserveCommand(name) { block.toJavaFuture(it) }

    fun onGoOnline(block: suspend CoroutineScope.() -> Unit) =
            scriptDsl.onGoOnline { block.toJavaFutureVoid() }

    fun onGoOffline(block: suspend CoroutineScope.() -> Unit) =
            scriptDsl.onGoOffline { block.toJavaFutureVoid() }

    fun onAbortSequence(block: suspend CoroutineScope.() -> Unit) =
            scriptDsl.onAbortSequence { block.toJavaFutureVoid() }

    fun onShutdown(block: suspend CoroutineScope.() -> Unit) =
            scriptDsl.onShutdown { block.toJavaFutureVoid() }

    fun onDiagnosticMode(block: suspend (UTCTime, String) -> Unit) =
            scriptDsl.onDiagnosticMode { x: UTCTime, y: String ->
                coroutineScope.launch { block(x, y) }.asCompletableFuture().thenAccept { }
            }

    fun onOperationsMode(block: suspend CoroutineScope.() -> Unit) =
            scriptDsl.onOperationsMode { block.toJavaFutureVoid() }

    fun onStop(block: suspend CoroutineScope.() -> Unit) =
            scriptDsl.onStop { block.toJavaFutureVoid() }

    fun onException(block: suspend CoroutineScope.(Throwable) -> Unit) =
            scriptDsl.onException {
                // "future" is used to swallow the exception coming from exception handlers
                coroutineScope.future { block(it) }
                        .exceptionally { log("Exception is thrown from Exception handler with message : ${it.message}") }
                        .thenAccept { }
            }

    fun loadScripts(vararg reusableScriptResult: ReusableScriptResult) =
            reusableScriptResult.forEach {
                this.scriptDsl.merge(it(cswServices, strandEc, coroutineScope).scriptDsl)
            }

    // fixme: use logging service
    fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")

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

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        log("Exception thrown in script with message: ${exception.message}")
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

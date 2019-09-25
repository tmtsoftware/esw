package esw.ocs.dsl.core

import akka.actor.typed.ActorSystem
import csw.command.client.CommandResponseManager
import csw.event.api.javadsl.IEventService
import csw.location.api.javadsl.ILocationService
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Observe
import csw.params.commands.Sequence
import csw.params.commands.SequenceCommand
import csw.params.commands.Setup
import csw.time.scheduler.api.TimeServiceScheduler
import esw.dsl.script.CswServices
import esw.dsl.script.ScriptDsl
import esw.ocs.api.SequencerAdminFactoryApi
import esw.ocs.dsl.highlevel.CswHighLevelDsl
import esw.ocs.dsl.nullable
import esw.ocs.macros.StrandEc
import java.util.concurrent.CompletionStage
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import kotlinx.coroutines.launch

sealed class ScriptDslKt : CoroutineScope, CswHighLevelDsl {

    abstract val cswServices: CswServices

    override val actorSystem: ActorSystem<*> by lazy { cswServices.actorSystem() }
    override val locationService: ILocationService by lazy { cswServices.locationService() }
    override val eventService: IEventService by lazy { cswServices.eventService() }
    override val crm: CommandResponseManager by lazy { cswServices.crm() }
    override val sequencerAdminFactory: SequencerAdminFactoryApi by lazy {
        cswServices.sequencerAdminFactory()
    }

    // this needs to be lazy otherwise handlers does not get loaded properly
    val scriptDsl: ScriptDsl by lazy { ScriptDslFactory.make(cswServices, strandEc()) }

    fun initialize(block: suspend () -> Unit) = launch { block() }

    suspend fun nextIf(predicate: (SequenceCommand) -> Boolean): SequenceCommand? =
        scriptDsl.jNextIf { predicate(it) }.await().nullable()

    fun handleSetup(name: String, block: suspend (Setup) -> Unit) {
        scriptDsl.jHandleSetupCommand(name) { block.toJavaFuture(it) }
    }

    fun handleObserve(name: String, block: suspend (Observe) -> Unit) {
        scriptDsl.jHandleObserveCommand(name) { block.toJavaFuture(it) }
    }

    fun handleGoOnline(block: suspend () -> Unit) {
        scriptDsl.jHandleGoOnline { block.toJavaFutureVoid() }
    }

    fun handleGoOffline(block: suspend () -> Unit) {
        scriptDsl.jHandleGoOffline { block.toJavaFutureVoid() }
    }

    fun handleAbort(block: suspend () -> Unit) {
        scriptDsl.jHandleAbort { block.toJavaFutureVoid() }
    }

    fun handleShutdown(block: suspend () -> Unit) {
        scriptDsl.jHandleShutdown { block.toJavaFutureVoid() }
    }

    fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")

    fun loadScripts(vararg reusableScriptResult: ReusableScriptResult) {
        reusableScriptResult.forEach {
            this.scriptDsl.merge(it(cswServices, strandEc(), coroutineContext).scriptDsl)
        }
    }

    suspend fun submitSequence(sequencerName: String, observingMode: String, sequence: Sequence): SubmitResponse =
        TODO("cswServices.submitSequence(sequencerName, observingMode, sequence).await()")

    private fun (suspend () -> Unit).toJavaFutureVoid(): CompletionStage<Void> {
        val block = this
        return future { block() }.thenAccept { }
    }

    private fun <T> (suspend (T) -> Unit).toJavaFuture(value: T): CompletionStage<Void> {
        val block = this
        return future { block(value) }.thenAccept { }
    }
}

class ReusableScript(
    override val cswServices: CswServices,
    private val _strandEc: StrandEc,
    override val coroutineContext: CoroutineContext
) : ScriptDslKt() {
    override fun strandEc(): StrandEc = _strandEc

    override val timeServiceScheduler: TimeServiceScheduler by lazy {
        cswServices.timeServiceSchedulerFactory().make(_strandEc.ec())
    }
}

open class Script(final override val cswServices: CswServices) : ScriptDslKt() {
    private val _strandEc = StrandEc.apply()
    private val job = Job()
    private val dispatcher = _strandEc.executorService().asCoroutineDispatcher()

    override val timeServiceScheduler: TimeServiceScheduler by lazy {
        cswServices.timeServiceSchedulerFactory().make(_strandEc.ec())
    }

    override val coroutineContext: CoroutineContext
        get() = job + dispatcher

    override fun strandEc(): StrandEc = _strandEc

    fun close() {
        job.cancel()
        dispatcher.close()
    }
}

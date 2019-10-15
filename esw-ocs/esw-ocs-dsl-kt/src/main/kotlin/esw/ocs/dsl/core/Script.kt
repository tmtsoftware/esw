package esw.ocs.dsl.core

import akka.actor.typed.ActorSystem
import csw.alarm.api.javadsl.IAlarmService
import csw.command.client.CommandResponseManager
import csw.event.api.javadsl.IEventPublisher
import csw.event.api.javadsl.IEventService
import csw.event.api.javadsl.IEventSubscriber
import csw.location.api.javadsl.ILocationService
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Observe
import csw.params.commands.Sequence
import csw.params.commands.SequenceCommand
import csw.params.commands.Setup
import csw.time.core.models.UTCTime
import csw.time.scheduler.api.TimeServiceScheduler
import esw.ocs.api.SequencerAdminFactoryApi
import esw.ocs.dsl.highlevel.AlarmSeverityData
import esw.ocs.dsl.highlevel.CommonUtils
import esw.ocs.dsl.highlevel.CswHighLevelDsl
import esw.ocs.dsl.nullable
import esw.ocs.dsl.script.CswServices
import esw.ocs.dsl.script.JScriptDsl
import esw.ocs.dsl.script.StrandEc
import esw.ocs.dsl.script.utils.LockUnlockUtil
import esw.ocs.dsl.sequence_manager.LocationServiceUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

sealed class ScriptDslKt : CswHighLevelDsl {

    abstract override val coroutineScope: CoroutineScope
    abstract val cswServices: CswServices
    val eventService: IEventService by lazy { cswServices.eventService() }

    override val actorSystem: ActorSystem<*> by lazy { cswServices.actorSystem() }
    override val locationService: ILocationService by lazy { cswServices.locationService() }
    private val locationServiceUtil: LocationServiceUtil by lazy {
        LocationServiceUtil(locationService.asScala(), actorSystem)
    }

    override val defaultPublisher: IEventPublisher by lazy { eventService.defaultPublisher() }
    override val defaultSubscriber: IEventSubscriber by lazy { eventService.defaultSubscriber() }

    override val alarmService: IAlarmService by lazy { cswServices.alarmService() }
    override val alarmSeverityData: AlarmSeverityData by lazy { AlarmSeverityData(HashMap()) }

    override val crm: CommandResponseManager by lazy { cswServices.crm() }
    private val sequencerAdminFactory: SequencerAdminFactoryApi by lazy {
        cswServices.sequencerAdminFactory()
    }

    // fixme: should not be visible from script
    override val commonUtils: CommonUtils by lazy { CommonUtils(sequencerAdminFactory, locationServiceUtil) }

    override val lockUnlockUtil: LockUnlockUtil by lazy { cswServices.lockUnlockUtil() }

    // this needs to be lazy otherwise handlers does not get loaded properly
    val scriptDsl: JScriptDsl by lazy { ScriptDslFactory.make(cswServices, strandEc()) }

    fun initialize(block: suspend () -> Unit) = coroutineScope.launch { block() }

    suspend fun nextIf(predicate: (SequenceCommand) -> Boolean): SequenceCommand? =
            scriptDsl.nextIf { predicate(it) }.await().nullable()

    fun handleSetup(name: String, block: suspend (Setup) -> Unit) {
        scriptDsl.handleSetupCommand(name) { block.toJavaFuture(it) }
    }

    fun handleObserve(name: String, block: suspend (Observe) -> Unit) {
        scriptDsl.handleObserveCommand(name) { block.toJavaFuture(it) }
    }

    fun handleGoOnline(block: suspend () -> Unit) {
        scriptDsl.handleGoOnline { block.toJavaFutureVoid() }
    }

    fun handleGoOffline(block: suspend () -> Unit) {
        scriptDsl.handleGoOffline { block.toJavaFutureVoid() }
    }

    fun handleAbort(block: suspend () -> Unit) {
        scriptDsl.handleAbort { block.toJavaFutureVoid() }
    }

    fun handleShutdown(block: suspend () -> Unit) {
        scriptDsl.handleShutdown { block.toJavaFutureVoid() }
    }

    fun handleDiagnosticMode(block: suspend (UTCTime, String) -> Unit) {
        scriptDsl.handleDiagnosticMode { x: UTCTime, y: String -> coroutineScope.future { block(x, y) }.thenAccept { } }
    }

    fun handleOperationsMode(block: suspend () -> Unit) {
        scriptDsl.handleOperationsMode { block.toJavaFutureVoid() }
    }

    fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")

    fun loadScripts(vararg reusableScriptResult: ReusableScriptResult) {
        reusableScriptResult.forEach {
            this.scriptDsl.merge(it(cswServices, strandEc(), coroutineScope).scriptDsl)
        }
    }

    suspend fun submitSequence(sequencerName: String, observingMode: String, sequence: Sequence): SubmitResponse =
            this.scriptDsl.submitSequence(sequencerName, observingMode, sequence).await()

    private fun (suspend () -> Unit).toJavaFutureVoid(): CompletionStage<Void> {
        val block = this
        return coroutineScope.future { block() }
                .whenComplete { v, e ->
                    if (e == null) {
                        CompletableFuture.completedFuture(v)
                    } else {
                        log("exception : ${e.message}")
                        //fixme: call exception handlers whenever implemented
                        CompletableFuture.failedFuture<Unit>(e)
                    }
                }
                .thenAccept { }
    }

    private fun <T> (suspend (T) -> Unit).toJavaFuture(value: T): CompletionStage<Void> {
        val block = this
        return suspend { block(value) }.toJavaFutureVoid()
    }
}

class ReusableScript(
        override val cswServices: CswServices,
        private val _strandEc: StrandEc,
        override val coroutineScope: CoroutineScope
) : ScriptDslKt() {
    override fun strandEc(): StrandEc = _strandEc

    override val timeServiceScheduler: TimeServiceScheduler by lazy {
        cswServices.timeServiceSchedulerFactory().make(_strandEc.ec())
    }
}

open class Script(final override val cswServices: CswServices) : ScriptDslKt() {
    private val _strandEc = StrandEc.apply()
    private val supervisorJob = SupervisorJob()
    private val dispatcher = _strandEc.executorService().asCoroutineDispatcher()

    override val timeServiceScheduler: TimeServiceScheduler by lazy {
        cswServices.timeServiceSchedulerFactory().make(_strandEc.ec())
    }
    private val exceptionHandler = CoroutineExceptionHandler {
        //fixme: call exception handlers whenever implemented
        _, exception ->
        log("Exception: ${exception.message}")
    }
    override val coroutineScope: CoroutineScope get() = CoroutineScope(supervisorJob + dispatcher + exceptionHandler)

    override fun strandEc(): StrandEc = _strandEc

    fun close() {
        supervisorJob.cancel()
        dispatcher.close()
    }
}

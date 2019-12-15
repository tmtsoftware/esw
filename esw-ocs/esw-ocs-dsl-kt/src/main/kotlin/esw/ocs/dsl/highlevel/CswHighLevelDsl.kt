package esw.ocs.dsl.highlevel

import akka.actor.typed.ActorSystem
import akka.actor.typed.SpawnProtocol
import csw.alarm.api.javadsl.IAlarmService
import csw.config.api.javadsl.IConfigClientService
import csw.database.DatabaseServiceFactory
import csw.event.api.javadsl.IEventPublisher
import csw.event.api.javadsl.IEventSubscriber
import csw.location.api.javadsl.ILocationService
import csw.location.api.javadsl.JComponentType
import csw.location.models.ComponentType
import csw.logging.api.javadsl.ILogger
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem
import csw.time.scheduler.api.TimeServiceScheduler
import esw.ocs.dsl.epics.CommandFlag
import esw.ocs.dsl.epics.Fsm
import esw.ocs.dsl.epics.FsmImpl
import esw.ocs.dsl.epics.FsmScope
import esw.ocs.dsl.internal.CswServices
import esw.ocs.dsl.script.StrandEc
import esw.ocs.dsl.script.utils.LockUnlockUtil
import esw.ocs.dsl.script.utils.SubsystemFactory
import esw.ocs.impl.core.script.ScriptContext
import esw.ocs.impl.internal.LocationServiceUtil
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration
import kotlin.time.toKotlinDuration

interface CswHighLevelDslApi : EventServiceDsl, TimeServiceDsl, CommandServiceDsl,
        ConfigServiceDsl, AlarmServiceDsl, LoopDsl, LoggingDsl, DatabaseServiceDsl {

    fun Assembly(prefix: String, defaultTimeout: Duration): RichComponent
    fun Hcd(prefix: String, defaultTimeout: Duration): RichComponent
    fun Sequencer(subsystem: String, observingMode: String, defaultTimeout: Duration): RichSequencer

    suspend fun Fsm(name: String, initState: String, block: suspend FsmScope.() -> Unit): Fsm
    fun commandFlag(): CommandFlag

    fun finishWithError(message: String = ""): Nothing = throw RuntimeException(message)
}

abstract class CswHighLevelDsl(val cswServices: CswServices, private val scriptContext: ScriptContext) : CswHighLevelDslApi {
    abstract val strandEc: StrandEc
    abstract override val coroutineScope: CoroutineScope

    final override val system: ActorSystem<SpawnProtocol.Command> = scriptContext.actorSystem()
    final override val locationService: ILocationService by lazy { cswServices.locationService }
    final override val configClient: IConfigClientService by lazy { cswServices.configClient }
    final override val logger: ILogger by lazy { cswServices.logger }
    final override val databaseServiceFactory: DatabaseServiceFactory by lazy { cswServices.databaseServiceFactory }
    final override val alarmService: IAlarmService by lazy { cswServices.alarmService }

    final override val defaultPublisher: IEventPublisher by lazy { cswServices.eventPublisher }
    final override val defaultSubscriber: IEventSubscriber by lazy { cswServices.eventSubscriber }
    final override val timeServiceScheduler: TimeServiceScheduler by lazy { cswServices.timeServiceScheduler }

    private val locationServiceUtil: LocationServiceUtil by lazy { LocationServiceUtil(cswServices.locationService.asScala(), system) }
    private val lockUnlockUtil: LockUnlockUtil by lazy { LockUnlockUtil(scriptContext.prefix(), system) }

    private val alarmConfig = scriptContext.config().getConfig("csw-alarm")
    override val _alarmRefreshDuration: Duration = alarmConfig.getDuration("refresh-interval").toKotlinDuration()

    /******** Command Service helpers ********/
    private fun richComponent(prefix: String, componentType: ComponentType, defaultTimeout: Duration): RichComponent =
            RichComponent(Prefix.apply(prefix), componentType, lockUnlockUtil, locationServiceUtil, system, defaultTimeout, coroutineScope)

    private fun richSequencer(subsystem: Subsystem, observingMode: String, defaultTimeout: Duration): RichSequencer =
            RichSequencer(subsystem, observingMode, scriptContext.sequencerApiFactory(), defaultTimeout, coroutineScope)

    override fun Assembly(prefix: String, defaultTimeout: Duration): RichComponent = richComponent(prefix, JComponentType.Assembly(), defaultTimeout)
    override fun Hcd(prefix: String, defaultTimeout: Duration): RichComponent = richComponent(prefix, JComponentType.HCD(), defaultTimeout)
    override fun Sequencer(subsystem: String, observingMode: String, defaultTimeout: Duration): RichSequencer = richSequencer(SubsystemFactory.make(subsystem), observingMode, defaultTimeout)

    /************* Fsm helpers **********/
    override suspend fun Fsm(name: String, initState: String, block: suspend FsmScope.() -> Unit): Fsm =
            FsmImpl(name, initState, coroutineScope, this).apply { block() }

    override fun commandFlag(): CommandFlag = CommandFlag()

}

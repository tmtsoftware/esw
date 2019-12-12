package esw.ocs.dsl.highlevel

import akka.actor.typed.ActorSystem
import csw.alarm.api.javadsl.IAlarmService
import csw.config.api.javadsl.IConfigClientService
import csw.database.DatabaseServiceFactory
import csw.event.api.javadsl.IEventPublisher
import csw.event.api.javadsl.IEventSubscriber
import csw.location.api.javadsl.ILocationService
import csw.location.api.javadsl.JComponentType
import csw.location.models.ComponentType
import csw.logging.api.javadsl.ILogger
import csw.params.core.models.Prefix
import csw.params.core.models.Subsystem
import csw.time.scheduler.api.TimeServiceScheduler
import esw.ocs.dsl.epics.CommandFlag
import esw.ocs.dsl.epics.Fsm
import esw.ocs.dsl.epics.FsmImpl
import esw.ocs.dsl.epics.FsmScope
import esw.ocs.dsl.script.CswServices
import esw.ocs.dsl.script.StrandEc
import esw.ocs.dsl.script.utils.SubsystemFactory
import esw.ocs.impl.internal.LocationServiceUtil
import kotlinx.coroutines.CoroutineScope

interface CswHighLevelDslApi : EventServiceDsl, TimeServiceDsl, CommandServiceDsl,
        ConfigServiceDsl, AlarmServiceDsl, LoopDsl, LoggingDsl, DatabaseServiceDsl {

    fun Assembly(name: String): RichComponent
    fun Hcd(name: String): RichComponent
    fun Sequencer(subsystem: String, observingMode: String): RichSequencer

    suspend fun Fsm(name: String, initState: String, block: suspend FsmScope.() -> Unit): Fsm
    fun commandFlag(): CommandFlag

    fun finishWithError(message: String = ""): Nothing = throw RuntimeException(message)
}

abstract class CswHighLevelDsl(private val cswServices: CswServices) : CswHighLevelDslApi {
    abstract val strandEc: StrandEc
    abstract override val coroutineScope: CoroutineScope

    final override val system: ActorSystem<*> = cswServices.actorSystem()
    final override val locationService: ILocationService = cswServices.locationService()
    final override val configClient: IConfigClientService = cswServices.configClientService()
    final override val logger: ILogger = cswServices.jLogger()
    final override val databaseServiceFactory: DatabaseServiceFactory = cswServices.databaseServiceFactory()
    final override val alarmService: IAlarmService = cswServices.alarmService()

    final override val defaultPublisher: IEventPublisher by lazy { cswServices.eventService().defaultPublisher() }
    final override val defaultSubscriber: IEventSubscriber by lazy { cswServices.eventService().defaultSubscriber() }
    final override val timeServiceScheduler: TimeServiceScheduler by lazy { cswServices.timeServiceSchedulerFactory().make(strandEc.ec()) }

    private val locationServiceUtil = LocationServiceUtil(cswServices.locationService().asScala(), system)
    /******** Command Service helpers ********/
    private fun richComponent(prefix: String, componentType: ComponentType): RichComponent =
            RichComponent(Prefix.apply(prefix), componentType, cswServices.lockUnlockUtil(), locationServiceUtil, system, coroutineScope)

    private fun richSequencer(subsystem: Subsystem, observingMode: String): RichSequencer =
            RichSequencer(subsystem, observingMode, cswServices.sequencerApiFactory(), coroutineScope)

    override fun Assembly(name: String): RichComponent = richComponent(name, JComponentType.Assembly())
    override fun Hcd(name: String): RichComponent = richComponent(name, JComponentType.HCD())
    override fun Sequencer(subsystem: String, observingMode: String): RichSequencer = richSequencer(SubsystemFactory.make(subsystem), observingMode)

    /************* Fsm helpers **********/
    override suspend fun Fsm(name: String, initState: String, block: suspend FsmScope.() -> Unit): Fsm =
            FsmImpl(name, initState, coroutineScope, this).apply { block() }

    override fun commandFlag(): CommandFlag = CommandFlag()

}

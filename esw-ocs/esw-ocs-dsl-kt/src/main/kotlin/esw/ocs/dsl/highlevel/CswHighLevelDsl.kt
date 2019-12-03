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
import csw.time.scheduler.api.TimeServiceScheduler
import esw.ocs.dsl.epics.CommandFlag
import esw.ocs.dsl.epics.FSMScope
import esw.ocs.dsl.epics.StateMachine
import esw.ocs.dsl.epics.StateMachineImpl
import esw.ocs.dsl.jdk.SuspendToJavaConverter
import esw.ocs.dsl.script.CswServices
import esw.ocs.dsl.script.StrandEc
import esw.ocs.dsl.sequence_manager.LocationServiceUtil
import kotlinx.coroutines.CoroutineScope

interface CswHighLevelDslApi : EventServiceDsl, TimeServiceDsl, CommandServiceDsl,
        ConfigServiceDsl, AlarmServiceDsl, LoopDsl, LoggingDsl, DatabaseServiceDsl {

    fun Assembly(name: String): RichComponent
    fun HCD(name: String): RichComponent
    fun Sequencer(sequencerId: String, observingMode: String): RichSequencer

    suspend fun FSM(name: String, initState: String, block: suspend FSMScope.() -> Unit): StateMachine
    fun commandFlag(): CommandFlag

    fun finishWithError(message: String = ""): Nothing = throw RuntimeException(message)
}

abstract class CswHighLevelDsl(private val cswServices: CswServices) : CswHighLevelDslApi {
    abstract val strandEc: StrandEc
    abstract override val coroutineScope: CoroutineScope

    final override val prefix: Prefix = cswServices.prefix()
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
    private fun richComponent(name: String, componentType: ComponentType): RichComponent =
            RichComponent(name, componentType, prefix, cswServices.lockUnlockUtil(), locationServiceUtil, system, coroutineScope)

    private fun richSequencer(sequencerId: String, observingMode: String): RichSequencer =
            RichSequencer(sequencerId, observingMode, cswServices.sequencerApiFactory())

    override fun Assembly(name: String): RichComponent = richComponent(name, JComponentType.Assembly())
    override fun HCD(name: String): RichComponent = richComponent(name, JComponentType.HCD())
    override fun Sequencer(sequencerId: String, observingMode: String): RichSequencer = richSequencer(sequencerId, observingMode)

    /************* FSM helpers **********/
    override suspend fun FSM(name: String, initState: String, block: suspend FSMScope.() -> Unit): StateMachine =
            StateMachineImpl(name, initState, coroutineScope).apply { block() }

    override fun commandFlag(): CommandFlag = CommandFlag()

}

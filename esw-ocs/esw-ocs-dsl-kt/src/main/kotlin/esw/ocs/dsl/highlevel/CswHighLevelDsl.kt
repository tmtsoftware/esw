package esw.ocs.dsl.highlevel

import akka.actor.typed.ActorSystem
import csw.alarm.api.javadsl.IAlarmService
import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
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
import esw.ocs.dsl.epics.FSMTopLevel
import esw.ocs.dsl.epics.StateMachine
import esw.ocs.dsl.epics.StateMachineImpl
import esw.ocs.dsl.jdk.SuspendToJavaConverter
import esw.ocs.dsl.script.CswServices
import esw.ocs.dsl.script.StrandEc
import esw.ocs.dsl.sequence_manager.LocationServiceUtil
import kotlinx.coroutines.CoroutineScope

abstract class CswHighLevelDsl(private val cswServices: CswServices) : EventServiceDsl, TimeServiceDsl, CommandServiceDsl,
        ConfigServiceDsl, AlarmServiceDsl, LoopDsl, SuspendToJavaConverter, LoggingDsl, DatabaseServiceDsl {
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
    private fun richComponent(prefix: String, componentType: ComponentType): RichComponent =
            RichComponent(Prefix.apply(prefix), componentType, this.prefix, cswServices.lockUnlockUtil(), locationServiceUtil, system, coroutineScope)

    private fun richSequencer(sequencerId: String, observingMode: String): RichSequencer =
            RichSequencer(sequencerId, observingMode, cswServices.sequencerApiFactory())

    fun Assembly(name: String): RichComponent = richComponent(name, JComponentType.Assembly())
    fun HCD(name: String): RichComponent = richComponent(name, JComponentType.HCD())
    fun Sequencer(sequencerId: String, observingMode: String): RichSequencer = richSequencer(sequencerId, observingMode)

    /************* FSM helpers **********/
    suspend fun FSM(name: String, initState: String, block: suspend FSMTopLevel.() -> Unit): StateMachine =
            StateMachineImpl(name, initState, coroutineScope).apply { block() }

    fun commandFlag() = CommandFlag()

}

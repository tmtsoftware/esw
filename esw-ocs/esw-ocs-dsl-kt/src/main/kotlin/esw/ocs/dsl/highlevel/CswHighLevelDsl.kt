package esw.ocs.dsl.highlevel

import akka.actor.typed.ActorSystem
import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import csw.config.api.javadsl.IConfigClientService
import csw.event.api.javadsl.IEventPublisher
import csw.event.api.javadsl.IEventSubscriber
import csw.location.api.javadsl.JComponentType
import csw.location.models.ComponentType
import csw.logging.api.javadsl.ILogger
import csw.params.core.models.Prefix
import csw.time.scheduler.api.TimeServiceScheduler
import esw.ocs.dsl.jdk.SuspendToJavaConverter
import esw.ocs.dsl.script.CswServices
import esw.ocs.dsl.script.StrandEc
import esw.ocs.dsl.sequence_manager.LocationServiceUtil
import kotlinx.coroutines.CoroutineScope

abstract class CswHighLevelDsl(private val cswServices: CswServices) : EventServiceDsl, TimeServiceDsl, CommandServiceDsl,
        ConfigServiceDsl, AlarmServiceDsl, LoopDsl, SuspendToJavaConverter, LoggingDsl {
    abstract val strandEc: StrandEc
    abstract override val coroutineScope: CoroutineScope

    final override val system: ActorSystem<*> = cswServices.actorSystem()
    private val locationServiceUtil = LocationServiceUtil(cswServices.locationService().asScala(), system)
    final override val defaultPublisher: IEventPublisher by lazy { cswServices.eventService().defaultPublisher() }
    final override val defaultSubscriber: IEventSubscriber by lazy { cswServices.eventService().defaultSubscriber() }

    final override val prefix: Prefix = cswServices.prefix()
    final override val logger: ILogger = cswServices.jLogger()

    final override val timeServiceScheduler: TimeServiceScheduler by lazy { cswServices.timeServiceSchedulerFactory().make(strandEc.ec()) }
    final override val configClient: IConfigClientService by lazy { cswServices.configClientService() }

    /***** AlarmServiceDSl impl *****/
    private val alarmServiceDslImpl by lazy { AlarmServiceDslImpl(cswServices.alarmService(), coroutineScope) }

    override fun setSeverity(alarmKey: AlarmKey, severity: AlarmSeverity) = alarmServiceDslImpl.setSeverity(alarmKey, severity)

    /******** Command Service helpers ********/
    private fun richComponent(name: String, componentType: ComponentType): RichComponent =
            RichComponent(name, componentType, cswServices.lockUnlockUtil(), locationServiceUtil, system, coroutineScope)

    private fun richSequencer(sequencerId: String, observingMode: String): RichSequencer =
            RichSequencer(sequencerId, observingMode, cswServices.sequencerAdminFactory(), cswServices.sequencerCommandFactory(), locationServiceUtil, system)

    fun Assembly(name: String): RichComponent = richComponent(name, JComponentType.Assembly())
    fun HCD(name: String): RichComponent = richComponent(name, JComponentType.HCD())
    fun Sequencer(sequencerId: String, observingMode: String): RichSequencer = richSequencer(sequencerId, observingMode)

}

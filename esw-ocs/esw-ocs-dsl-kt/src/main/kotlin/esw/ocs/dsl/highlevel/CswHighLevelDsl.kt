package esw.ocs.dsl.highlevel

import akka.actor.typed.ActorSystem
import csw.command.client.CommandResponseManager
import csw.command.client.ICommandServiceFactory
import csw.config.api.javadsl.IConfigClientService
import csw.event.api.javadsl.IEventPublisher
import csw.event.api.javadsl.IEventSubscriber
import csw.location.api.javadsl.ILocationService
import csw.time.scheduler.api.TimeServiceScheduler
import esw.ocs.dsl.script.CswServices
import esw.ocs.dsl.script.StrandEc
import esw.ocs.dsl.script.utils.LockUnlockUtil
import esw.ocs.dsl.sequence_manager.LocationServiceUtil
import kotlinx.coroutines.CoroutineScope

abstract class CswHighLevelDsl(private val cswServices: CswServices) : EventServiceDsl, TimeServiceDsl, CommandServiceDsl, CrmDsl, DiagnosticDsl,
        LockUnlockDsl, OnlineOfflineDsl, AbortSequenceDsl, StopDsl, ConfigServiceDsl,
        AlarmServiceDsl by AlarmServiceDslImpl(cswServices.alarmService()) {
    abstract val strandEc: StrandEc
    abstract override val coroutineScope: CoroutineScope

    final override val actorSystem: ActorSystem<*> = cswServices.actorSystem()
    final override val locationService: ILocationService = cswServices.locationService()
    final override val defaultPublisher: IEventPublisher by lazy { cswServices.eventService().defaultPublisher() }
    final override val defaultSubscriber: IEventSubscriber by lazy { cswServices.eventService().defaultSubscriber() }
    final override val crm: CommandResponseManager = cswServices.crm()
    // fixme: should not be visible from script
    final override val commonUtils: CommonUtils = CommonUtils(cswServices.sequencerAdminFactory(), LocationServiceUtil(locationService.asScala(), actorSystem))
    final override val lockUnlockUtil: LockUnlockUtil = cswServices.lockUnlockUtil()

    final override val timeServiceScheduler: TimeServiceScheduler by lazy {
        cswServices.timeServiceSchedulerFactory().make(strandEc.ec())
    }

    final override val configClientService: IConfigClientService by lazy {
        cswServices.configClientService()
    }
}

package esw.ocs.dsl.script

import akka.actor.typed.ActorSystem
import csw.alarm.api.javadsl.IAlarmService
import csw.command.client.CommandResponseManager
import csw.config.api.javadsl.IConfigClientService
import csw.event.api.javadsl.IEventService
import csw.location.api.javadsl.ILocationService
import csw.time.scheduler.TimeServiceSchedulerFactory
import esw.ocs.api.SequencerAdminFactoryApi
import esw.ocs.dsl.script.utils.LockUnlockUtil

class CswServices(
    private[esw] val sequenceOperatorFactory: () => SequenceOperator,
    val crm: CommandResponseManager,
    val actorSystem: ActorSystem[_],
    val locationService: ILocationService,
    val eventService: IEventService,
    private[esw] val timeServiceSchedulerFactory: TimeServiceSchedulerFactory,
    private[esw] val sequencerAdminFactory: SequencerAdminFactoryApi,
    private[esw] val lockUnlockUtil: LockUnlockUtil,
    private[esw] val configClientService: IConfigClientService,
    val alarmService: IAlarmService
)

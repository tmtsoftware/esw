package esw.ocs.dsl.script

import akka.actor.typed.ActorSystem
import csw.alarm.api.javadsl.IAlarmService
import csw.config.api.javadsl.IConfigClientService
import csw.event.api.javadsl.IEventService
import csw.location.api.javadsl.ILocationService
import csw.logging.api.javadsl.ILogger
import csw.params.core.models.Prefix
import csw.time.scheduler.TimeServiceSchedulerFactory
import esw.ocs.api.{SequencerAdminFactoryApi, SequencerCommandFactoryApi}
import esw.ocs.dsl.script.utils.LockUnlockUtil

class CswServices(
    val prefix: Prefix,
    val sequenceOperatorFactory: () => SequenceOperator,
    val jLogger: ILogger,
    val actorSystem: ActorSystem[_],
    val locationService: ILocationService,
    val eventService: IEventService,
    val timeServiceSchedulerFactory: TimeServiceSchedulerFactory,
    val sequencerAdminFactory: SequencerAdminFactoryApi,
    val sequencerCommandFactory: SequencerCommandFactoryApi,
    val lockUnlockUtil: LockUnlockUtil,
    val configClientService: IConfigClientService,
    val alarmService: IAlarmService
)

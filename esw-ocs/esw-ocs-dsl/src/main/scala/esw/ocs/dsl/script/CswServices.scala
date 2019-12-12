package esw.ocs.dsl.script

import akka.actor.typed.ActorSystem
import csw.alarm.api.javadsl.IAlarmService
import csw.config.api.javadsl.IConfigClientService
import csw.database.DatabaseServiceFactory
import csw.event.api.javadsl.IEventService
import csw.location.api.javadsl.ILocationService
import csw.logging.api.javadsl.ILogger
import csw.time.scheduler.TimeServiceSchedulerFactory
import esw.ocs.dsl.script.utils.LockUnlockUtil
import esw.ocs.impl.SequencerActorProxyFactory
import esw.ocs.impl.core.api.SequenceOperator

class CswServices(
    val sequenceOperatorFactory: () => SequenceOperator,
    val jLogger: ILogger,
    val actorSystem: ActorSystem[_],
    val locationService: ILocationService,
    val eventService: IEventService,
    val timeServiceSchedulerFactory: TimeServiceSchedulerFactory,
    val sequencerApiFactory: SequencerActorProxyFactory,
    val databaseServiceFactory: DatabaseServiceFactory,
    val lockUnlockUtil: LockUnlockUtil,
    val configClientService: IConfigClientService,
    val alarmService: IAlarmService
)

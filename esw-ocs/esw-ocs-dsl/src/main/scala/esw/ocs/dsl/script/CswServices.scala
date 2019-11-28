package esw.ocs.dsl.script

import java.util.concurrent.CompletionStage
import java.util.function.BiFunction

import akka.actor.typed.ActorSystem
import csw.alarm.api.javadsl.IAlarmService
import csw.config.api.javadsl.IConfigClientService
import csw.database.DatabaseServiceFactory
import csw.event.api.javadsl.IEventService
import csw.location.api.javadsl.ILocationService
import csw.logging.api.javadsl.ILogger
import csw.params.core.models.Prefix
import csw.time.scheduler.TimeServiceSchedulerFactory
import esw.ocs.api.SequencerApi
import esw.ocs.dsl.script.utils.LockUnlockUtil

class CswServices(
    val prefix: Prefix,
    val sequenceOperatorFactory: () => SequenceOperator,
    val jLogger: ILogger,
    val actorSystem: ActorSystem[_],
    val locationService: ILocationService,
    val eventService: IEventService,
    val timeServiceSchedulerFactory: TimeServiceSchedulerFactory,
    val sequencerApiFactory: BiFunction[String, String, CompletionStage[SequencerApi]],
    val databaseServiceFactory: DatabaseServiceFactory,
    val lockUnlockUtil: LockUnlockUtil,
    val configClientService: IConfigClientService,
    val alarmService: IAlarmService
)

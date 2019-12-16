package esw.ocs.dsl.internal

import akka.actor.typed.ActorSystem
import akka.actor.typed.SpawnProtocol
import csw.alarm.api.javadsl.IAlarmService
import csw.config.api.javadsl.IConfigClientService
import csw.config.client.javadsl.JConfigClientFactory
import csw.database.DatabaseServiceFactory
import csw.event.api.javadsl.IEventPublisher
import csw.event.api.javadsl.IEventService
import csw.event.api.javadsl.IEventSubscriber
import csw.location.api.javadsl.ILocationService
import csw.location.client.javadsl.JHttpLocationServiceFactory
import csw.logging.api.javadsl.ILogger
import csw.time.scheduler.TimeServiceSchedulerFactory
import csw.time.scheduler.api.TimeServiceScheduler
import esw.ocs.dsl.script.StrandEc
import esw.ocs.impl.script.ScriptContext

class CswServices(ctx: ScriptContext, strandEc: StrandEc) {
    private val actorSystem: ActorSystem<SpawnProtocol.Command> = ctx.actorSystem()

    val logger: ILogger = ctx.jLogger()
    val alarmService: IAlarmService = ctx.alarmService()
    val eventService: IEventService = ctx.eventService()
    val eventPublisher: IEventPublisher by lazy { eventService.defaultPublisher() }
    val eventSubscriber: IEventSubscriber by lazy { eventService.defaultSubscriber() }
    val locationService: ILocationService by lazy { JHttpLocationServiceFactory.makeLocalClient(actorSystem) }
    val configClient: IConfigClientService by lazy { JConfigClientFactory.clientApi(actorSystem, locationService) }
    val timeServiceScheduler: TimeServiceScheduler by lazy { TimeServiceSchedulerFactory(actorSystem.scheduler()).make(strandEc.ec()) }
    val databaseServiceFactory: DatabaseServiceFactory by lazy { DatabaseServiceFactory(actorSystem) }
}
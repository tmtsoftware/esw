package esw.ocs.dsl.internal

import csw.alarm.api.javadsl.IAlarmService
import csw.config.client.javadsl.JConfigClientFactory
import csw.database.DatabaseServiceFactory
import csw.event.api.javadsl.IEventPublisher
import csw.event.api.javadsl.IEventSubscriber
import csw.location.api.javadsl.ILocationService
import csw.location.client.javadsl.JHttpLocationServiceFactory
import csw.logging.api.javadsl.ILogger
import csw.time.scheduler.TimeServiceSchedulerFactory
import csw.time.scheduler.api.TimeServiceScheduler
import esw.ocs.impl.core.script.ScriptContext
import esw.ocs.dsl.script.StrandEc
import esw.ocs.dsl.script.utils.LockUnlockUtil

class CswServices(val ctx: ScriptContext, strandEc: StrandEc) {
    val logger: ILogger = ctx.jLogger()
    val alarmService: IAlarmService = ctx.alarmService()
    val locationService: ILocationService by lazy { JHttpLocationServiceFactory.makeLocalClient(ctx.actorSystem()) }
    val timeServiceScheduler: TimeServiceScheduler by lazy { TimeServiceSchedulerFactory(ctx.actorSystem().scheduler()).make(strandEc.ec()) }
    val databaseServiceFactory: DatabaseServiceFactory by lazy { DatabaseServiceFactory(ctx.actorSystem()) }
    val lockUnlockUtil: LockUnlockUtil by lazy { LockUnlockUtil(ctx.prefix(), ctx.actorSystem()) }
    val defaultSubscriber: IEventSubscriber by lazy { ctx.eventService().defaultSubscriber() }
    val defaultPublisher: IEventPublisher by lazy { ctx.eventService().defaultPublisher() }
    val configClient by lazy { JConfigClientFactory.clientApi(ctx.actorSystem(), locationService) }
}
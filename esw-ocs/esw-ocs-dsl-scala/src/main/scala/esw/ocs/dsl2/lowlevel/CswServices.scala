package esw.ocs.dsl2.lowlevel

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.alarm.api.scaladsl.AlarmService
import csw.config.api.scaladsl.ConfigClientService
import csw.config.client.scaladsl.ConfigClientFactory
import csw.database.DatabaseServiceFactory
import csw.event.api.scaladsl.{EventPublisher, EventService, EventSubscriber}
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.logging.api.scaladsl.Logger
import esw.ocs.dsl.script.StrandEc
import esw.ocs.impl.script.ScriptContext
import csw.time.scheduler.TimeServiceSchedulerFactory
import csw.time.scheduler.api.TimeServiceScheduler

class CswServices(ctx: ScriptContext, strandEc: StrandEc) {
  val alarmService: AlarmService         = ctx.alarmService.asScala
  val eventService: EventService         = ctx.eventService.asScala
  val eventPublisher: EventPublisher     = eventService.defaultPublisher
  val eventSubscriber: EventSubscriber   = eventService.defaultSubscriber
  val locationService: LocationService   = HttpLocationServiceFactory.makeLocalClient(ctx.actorSystem)
  val configService: ConfigClientService = ConfigClientFactory.clientApi(ctx.actorSystem, locationService)
  val timeService: TimeServiceScheduler  = TimeServiceSchedulerFactory()(ctx.actorSystem.scheduler).make()(strandEc.ec)
  val databaseServiceFactory: DatabaseServiceFactory = new DatabaseServiceFactory(ctx.actorSystem)
}

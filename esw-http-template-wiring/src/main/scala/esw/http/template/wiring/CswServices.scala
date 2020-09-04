package esw.http.template.wiring

import akka.actor.typed.ActorSystem
import csw.alarm.api.javadsl.IAlarmService
import csw.alarm.api.scaladsl.AlarmService
import csw.alarm.client.internal.extensions.AlarmServiceExt.RichAlarmService
import csw.config.api.javadsl.IConfigClientService
import csw.config.api.scaladsl.ConfigClientService
import csw.config.client.javadsl.JConfigClientFactory
import csw.event.api.javadsl.IEventService
import csw.event.api.scaladsl.EventService
import csw.event.client.internal.commons.EventServiceExt.RichEventService
import csw.location.api.javadsl.ILocationService
import csw.location.api.scaladsl.LocationService
import csw.location.client.extensions.LocationServiceExt.RichLocationService
import csw.logging.client.javadsl.JLoggerFactory
import csw.logging.client.scaladsl.LoggerFactory
import csw.time.scheduler.api.TimeServiceScheduler

case class JCswServices(
    locationService: ILocationService,
    eventService: IEventService,
    alarmService: IAlarmService,
    timeServiceScheduler: TimeServiceScheduler,
    loggerFactory: JLoggerFactory,
    configClientService: IConfigClientService
)

case class CswServices(
    locationService: LocationService,
    eventService: EventService,
    alarmService: AlarmService,
    timeServiceScheduler: TimeServiceScheduler,
    loggerFactory: LoggerFactory,
    configClientService: ConfigClientService
) {
  def asJava(implicit actorSystem: ActorSystem[_]): JCswServices = {
    import actorSystem.executionContext
    val iLocationService = locationService.asJava
    JCswServices(
      iLocationService,
      eventService.asJava,
      alarmService.asJava,
      timeServiceScheduler,
      loggerFactory.asJava,
      JConfigClientFactory.clientApi(actorSystem, iLocationService)
    )
  }
}

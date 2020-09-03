package esw.http.template.wiring

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import csw.alarm.api.javadsl.IAlarmService
import csw.alarm.api.scaladsl.AlarmService
import csw.alarm.client.AlarmServiceFactory
import csw.config.api.javadsl.IConfigClientService
import csw.config.api.scaladsl.ConfigClientService
import csw.config.client.javadsl.JConfigClientFactory
import csw.event.api.javadsl.IEventService
import csw.event.api.scaladsl.EventService
import csw.event.client.EventServiceFactory
import csw.event.client.models.EventStores.RedisStore
import csw.location.api.javadsl.ILocationService
import csw.location.api.scaladsl.LocationService
import csw.location.client.extensions.LocationServiceExt.RichLocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.logging.client.javadsl.JLoggerFactory
import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.Prefix
import csw.time.scheduler.TimeServiceSchedulerFactory
import csw.time.scheduler.api.TimeServiceScheduler
import esw.http.core.commons.ServiceLogger
import io.lettuce.core.RedisClient

import scala.concurrent.Future
import scala.util.chaining.scalaUtilChainingOps

class JCswContext(prefix: Prefix)(implicit actorSystem: ActorSystem[_]) {
  import actorSystem.executionContext
  lazy val locationService: ILocationService         = HttpLocationServiceFactory.makeLocalClient(actorSystem).asJava
  lazy val configClientService: IConfigClientService = JConfigClientFactory.clientApi(actorSystem, locationService)
  private val _loggerFactory                         = new ServiceLogger(prefix)
  lazy val loggerFactory: JLoggerFactory             = _loggerFactory.asJava

  private lazy val redisClient: RedisClient                 = RedisClient.create().tap(shutdownRedisOnTermination)
  private lazy val eventServiceFactory: EventServiceFactory = new EventServiceFactory(RedisStore(redisClient))
  lazy val eventService: IEventService                      = eventServiceFactory.jMake(locationService, actorSystem)
  private lazy val alarmServiceFactory: AlarmServiceFactory = new AlarmServiceFactory(redisClient)
  lazy val alarmService: IAlarmService                      = alarmServiceFactory.jMakeClientApi(locationService, actorSystem)

  private lazy val timeServiceSchedulerFactory        = new TimeServiceSchedulerFactory()(actorSystem.scheduler)
  lazy val timeServiceScheduler: TimeServiceScheduler = timeServiceSchedulerFactory.make()

  private def shutdownRedisOnTermination(client: RedisClient)(implicit actorSystem: ActorSystem[_]): Unit = {
    import actorSystem.executionContext
    CoordinatedShutdown(actorSystem).addTask(
      CoordinatedShutdown.PhaseBeforeServiceUnbind,
      "redis-client-shutdown"
    )(() => Future { client.shutdown(); Done })
  }

  def asScala(): CswContext =
    CswContext(
      locationService.asScala,
      eventService.asScala,
      alarmService.asScala,
      timeServiceScheduler,
      _loggerFactory,
      configClientService.asScala,
      this
    )
}

case class CswContext(
    locationService: LocationService,
    eventService: EventService,
    alarmService: AlarmService,
    timeServiceScheduler: TimeServiceScheduler,
    loggerFactory: LoggerFactory,
    configClientService: ConfigClientService,
    private val jCswContext: JCswContext
) {
  def asJava(): JCswContext = jCswContext
}

object CswContext {
  def apply(prefix: Prefix)(implicit actorSystem: ActorSystem[_]): CswContext = new JCswContext(prefix).asScala()
}

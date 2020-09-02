package template

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import csw.alarm.api.scaladsl.AlarmService
import csw.alarm.client.AlarmServiceFactory
import csw.config.api.scaladsl.ConfigClientService
import csw.config.client.commons.ConfigUtils
import csw.config.client.scaladsl.ConfigClientFactory
import csw.event.api.scaladsl.EventService
import csw.event.client.EventServiceFactory
import csw.event.client.internal.commons.EventSubscriberUtil
import csw.event.client.models.EventStores.RedisStore
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.time.scheduler.TimeServiceSchedulerFactory
import io.lettuce.core.RedisClient

import scala.concurrent.{ExecutionContext, Future}
import scala.util.chaining.scalaUtilChainingOps

class CswWiring(implicit actorSystem: ActorSystem[_]) {

  lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient(actorSystem)

  lazy val configClientService: ConfigClientService = ConfigClientFactory.clientApi(actorSystem, locationService)
  lazy val configUtils: ConfigUtils                 = new ConfigUtils(configClientService)(actorSystem)

  lazy val eventSubscriberUtil: EventSubscriberUtil = new EventSubscriberUtil()
  lazy val eventServiceFactory: EventServiceFactory = new EventServiceFactory(RedisStore(redisClient))
  lazy val eventService: EventService               = eventServiceFactory.make(locationService)

  lazy val alarmServiceFactory: AlarmServiceFactory = new AlarmServiceFactory(redisClient)
  lazy val alarmService: AlarmService               = alarmServiceFactory.makeClientApi(locationService)

  lazy val timeServiceSchedulerFactory = new TimeServiceSchedulerFactory()(actorSystem.scheduler)

  lazy val redisClient: RedisClient = RedisClient.create().tap(shutdownRedisOnTermination)

  private def shutdownRedisOnTermination(client: RedisClient): Unit = {
    implicit val ec: ExecutionContext = actorSystem.executionContext

    CoordinatedShutdown(actorSystem).addTask(
      CoordinatedShutdown.PhaseBeforeServiceUnbind,
      "redis-client-shutdown"
    )(() => Future { client.shutdown(); Done })
  }
}

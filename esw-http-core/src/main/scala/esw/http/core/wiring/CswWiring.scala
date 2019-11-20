package esw.http.core.wiring

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.alarm.api.scaladsl.AlarmService
import csw.alarm.client.AlarmServiceFactory
import csw.command.client.CommandServiceFactory
import csw.config.api.scaladsl.ConfigClientService
import csw.config.client.commons.ConfigUtils
import csw.config.client.scaladsl.ConfigClientFactory
import csw.event.api.scaladsl.EventService
import csw.event.client.EventServiceFactory
import csw.event.client.internal.commons.EventSubscriberUtil
import csw.event.client.models.EventStores.RedisStore
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.time.scheduler.TimeServiceSchedulerFactory
import esw.http.core.utils.ComponentFactory
import io.lettuce.core.RedisClient

import scala.concurrent.{ExecutionContext, Future}

/**
 * Represents a class that lazily initializes necessary instances to run a component(s)
 */
class CswWiring() {
  lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "esw-system")
  lazy val actorRuntime: ActorRuntime                      = new ActorRuntime(actorSystem)
  import actorRuntime._

  lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient(actorSystem)

  lazy val configClientService: ConfigClientService = ConfigClientFactory.clientApi(actorSystem, locationService)
  lazy val configUtils: ConfigUtils                 = new ConfigUtils(configClientService)(actorSystem)

  lazy val eventSubscriberUtil: EventSubscriberUtil = new EventSubscriberUtil()
  lazy val eventServiceFactory: EventServiceFactory = new EventServiceFactory(RedisStore(redisClient))
  lazy val eventService: EventService               = eventServiceFactory.make(locationService)

  lazy val alarmServiceFactory: AlarmServiceFactory = new AlarmServiceFactory(redisClient)
  lazy val alarmService: AlarmService               = alarmServiceFactory.makeClientApi(locationService)

  lazy val componentFactory            = new ComponentFactory(locationService, CommandServiceFactory)
  lazy val timeServiceSchedulerFactory = new TimeServiceSchedulerFactory()(typedSystem.scheduler)

  lazy val redisClient: RedisClient = {
    val client = RedisClient.create()
    shutdownRedisOnTermination(client)
    client
  }

  private def shutdownRedisOnTermination(client: RedisClient): Unit = {
    implicit val ec: ExecutionContext = actorSystem.executionContext

    actorRuntime.coordinatedShutdown.addTask(
      CoordinatedShutdown.PhaseBeforeServiceUnbind,
      "redis-client-shutdown"
    )(() => Future { client.shutdown(); Done })
  }
}

object CswWiring {
  def make(_actorSystem: ActorSystem[SpawnProtocol.Command]): CswWiring = new CswWiring() {
    override lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = _actorSystem
  }
}

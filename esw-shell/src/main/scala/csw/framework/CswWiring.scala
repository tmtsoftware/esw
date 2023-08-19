package csw.framework

import csw.command.client.models.framework.{ComponentInfo, LocationServiceUsage}
import csw.framework.internal.wiring.{ActorRuntime, CswFrameworkSystem, FrameworkWiring}
import csw.framework.models.CswContext
import csw.location.api.models.ComponentType
import csw.prefix.models.Prefix
import esw.commons.extensions.FutureExt.FutureOps
import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.api.scaladsl.LocationService
import csw.event.client.EventServiceFactory
import csw.event.client.models.EventStores.RedisStore
import csw.alarm.client.AlarmServiceFactory
import io.lettuce.core.RedisClient
import org.apache.pekko.actor.CoordinatedShutdown
import org.apache.pekko.Done
import scala.concurrent.{ExecutionContext, Future}

class CswWiring {
  lazy val redisClient: RedisClient = {
    val client = RedisClient.create()
    shutdownRedisOnTermination(client)
  }

  lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "framework-system")
  final lazy val actorRuntime: ActorRuntime                = new ActorRuntime(actorSystem)
  lazy val locationService: LocationService                = HttpLocationServiceFactory.makeLocalClient(actorSystem)
  lazy val eventServiceFactory: EventServiceFactory        = new EventServiceFactory(RedisStore(redisClient))
  lazy val alarmServiceFactory: AlarmServiceFactory        = new AlarmServiceFactory(redisClient)

  private def shutdownRedisOnTermination(client: RedisClient): RedisClient = {
    implicit val ec: ExecutionContext = actorSystem.executionContext

    actorRuntime.coordinatedShutdown.addTask(
      CoordinatedShutdown.PhaseBeforeServiceUnbind,
      "redis-client-shutdown"
    )(() =>
      Future {
        client.shutdown();
        Done
      }
    )
    client
  }

  private implicit lazy val cswFrameworkSystem: CswFrameworkSystem = new CswFrameworkSystem(actorSystem)

  lazy val cswContext: CswContext =
    CswContext
      .make(
        locationService,
        eventServiceFactory,
        alarmServiceFactory,
        // dummy component info, it is not used by esw-esw.shell
        ComponentInfo(
          Prefix("csw.esw.shell"),
          ComponentType.Service,
          "",
          LocationServiceUsage.DoNotRegister
        )
      )
      .await()
}

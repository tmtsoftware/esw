package esw.ocs.framework

import akka.actor
import akka.actor.{CoordinatedShutdown, Scheduler}
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.stream.Materializer
import akka.stream.typed.scaladsl.ActorMaterializer
import akka.util.Timeout
import csw.location.client.ActorSystemFactory
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationLong

class ActorRuntime(componentName: String) {
  implicit lazy val typedSystem: ActorSystem[SpawnProtocol] =
    ActorSystemFactory.remote(SpawnProtocol.behavior, "sequencer-system")
  implicit val untypedSystem: actor.ActorSystem     = typedSystem.toUntyped
  implicit lazy val mat: Materializer               = ActorMaterializer()
  implicit lazy val ec: ExecutionContext            = typedSystem.executionContext
  implicit lazy val scheduler: Scheduler            = typedSystem.scheduler
  implicit lazy val timeout: Timeout                = 5.seconds
  lazy val coordinatedShutdown: CoordinatedShutdown = CoordinatedShutdown(untypedSystem)
  lazy val log: Logger                              = new LoggerFactory(componentName).getLogger
}

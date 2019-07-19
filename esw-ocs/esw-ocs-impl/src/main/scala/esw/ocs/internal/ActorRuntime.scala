package esw.ocs.internal

import akka.actor
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.actor.{CoordinatedShutdown, Scheduler}
import akka.stream.Materializer
import akka.stream.typed.scaladsl.ActorMaterializer
import akka.util.Timeout
import csw.location.client.ActorSystemFactory
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory

import scala.concurrent.ExecutionContext

private[internal] class ActorRuntime(componentName: String) {
  implicit lazy val typedSystem: ActorSystem[SpawnProtocol] =
    ActorSystemFactory.remote(SpawnProtocol.behavior, "sequencer-system")
  implicit lazy val untypedSystem: actor.ActorSystem = typedSystem.toUntyped
  implicit lazy val mat: Materializer                = ActorMaterializer()
  implicit lazy val ec: ExecutionContext             = typedSystem.executionContext
  implicit lazy val scheduler: Scheduler             = typedSystem.scheduler
  implicit lazy val timeout: Timeout                 = Timeouts.DefaultTimeout

  lazy val coordinatedShutdown: CoordinatedShutdown = CoordinatedShutdown(untypedSystem)
  lazy val loggerFactory                            = new LoggerFactory(componentName)
  lazy val log: Logger                              = loggerFactory.getLogger
}

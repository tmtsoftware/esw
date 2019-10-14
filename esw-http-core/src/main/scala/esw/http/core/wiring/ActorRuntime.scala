package esw.http.core.wiring

import akka.actor.CoordinatedShutdown.Reason
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, Scheduler, SpawnProtocol}
import akka.actor.CoordinatedShutdown
import akka.stream.Materializer
import akka.{Done, actor}
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.network.utils.Networks
import esw.http.core.BuildInfo

import scala.concurrent.{ExecutionContext, Future}

/**
 * A convenient class wrapping actor system and providing handles for execution context, materializer and clean up of actor system
 */
class ActorRuntime(_typedSystem: ActorSystem[SpawnProtocol.Command]) {
  implicit val typedSystem: ActorSystem[SpawnProtocol.Command] = _typedSystem
  implicit val untypedSystem: actor.ActorSystem                = _typedSystem.toClassic
  implicit val ec: ExecutionContext                            = typedSystem.executionContext
  implicit val mat: Materializer                               = Materializer(typedSystem)
  implicit lazy val scheduler: Scheduler                       = typedSystem.scheduler
  val coordinatedShutdown: CoordinatedShutdown                 = CoordinatedShutdown(untypedSystem)

  def startLogging(name: String, version: String = BuildInfo.version): LoggingSystem =
    LoggingSystemFactory.start(name, version, Networks().hostname, typedSystem)

  def shutdown(reason: Reason): Future[Done] = coordinatedShutdown.run(reason)
}

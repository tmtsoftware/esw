package esw.template.http.server.wiring

import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.Reason
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.stream.Materializer
import akka.stream.typed.scaladsl.ActorMaterializer
import akka.{Done, actor}
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.network.utils.Networks

import scala.concurrent.{ExecutionContext, Future}

/**
 * A convenient class wrapping actor system and providing handles for execution context, materializer and clean up of actor system
 */
class ActorRuntime(_typedSystem: ActorSystem[SpawnProtocol]) {
  implicit val typedSystem: ActorSystem[SpawnProtocol] = _typedSystem
  implicit val untypedSystem: actor.ActorSystem        = _typedSystem.toUntyped
  implicit val ec: ExecutionContext                    = typedSystem.executionContext
  implicit val mat: Materializer                       = ActorMaterializer()

  val coordinatedShutdown: CoordinatedShutdown = CoordinatedShutdown(untypedSystem)

  def startLogging(name: String, version: String): LoggingSystem =
    LoggingSystemFactory.start(name, version, Networks().hostname, typedSystem)

  def shutdown(reason: Reason): Future[Done] = coordinatedShutdown.run(reason)
}

package esw.template.http.server.commons

import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.Reason
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.stream.Materializer
import akka.stream.typed.scaladsl.ActorMaterializer
import akka.{Done, actor}

import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * A convenient class wrapping actor system and providing handles for execution context, materializer and clean up of actor system
 */
class ActorRuntime(_typedSystem: ActorSystem[SpawnProtocol]) {
  implicit val typedSystem: ActorSystem[SpawnProtocol] = _typedSystem
  implicit val untypedSystem: actor.ActorSystem        = _typedSystem.toUntyped
  implicit val ec: ExecutionContextExecutor            = typedSystem.executionContext
  implicit val mat: Materializer                       = ActorMaterializer()

  val coordinatedShutdown: CoordinatedShutdown = CoordinatedShutdown(untypedSystem)

  def shutdown(reason: Reason): Future[Done] = coordinatedShutdown.run(reason)
}

package esw.agent.app.utils

import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.Reason
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.{Done, actor}
import csw.location.client.ActorSystemFactory
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.network.utils.Networks

import scala.concurrent.{ExecutionContext, Future}

/**
 * A convenient class wrapping actor system and providing handles for execution context, materializer and clean up of actor system
 */
class ActorRuntime {
  implicit val typedSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "agent-app")
  implicit val untypedSystem: actor.ActorSystem                = typedSystem.toClassic
  implicit val ec: ExecutionContext                            = typedSystem.executionContext
  val coordinatedShutdown: CoordinatedShutdown                 = CoordinatedShutdown(untypedSystem)

  def withShutdownHook[T](f: => T): ActorRuntime = {
    coordinatedShutdown.addJvmShutdownHook(f)
    this
  }

  def startLogging(name: String, version: String): Unit = {
    LoggingSystemFactory.start(name, version, Networks().hostname, typedSystem)
  }

  def shutdown(reason: Reason): Future[Done] = coordinatedShutdown.run(reason)
}

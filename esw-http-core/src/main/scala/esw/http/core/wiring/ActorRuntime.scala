package esw.http.core.wiring

import org.apache.pekko.Done
import org.apache.pekko.actor.CoordinatedShutdown
import org.apache.pekko.actor.CoordinatedShutdown.Reason
import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
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
  implicit val ec: ExecutionContext                            = typedSystem.executionContext
  val coordinatedShutdown: CoordinatedShutdown                 = CoordinatedShutdown(typedSystem)

  def startLogging(name: String, version: String = BuildInfo.version): LoggingSystem =
    LoggingSystemFactory.start(name, version, Networks().hostname, typedSystem)

  def shutdown(reason: Reason): Future[Done] = coordinatedShutdown.run(reason)
}

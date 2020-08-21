package esw.http.core.wiring

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.models.ComponentType
import csw.location.client.ActorSystemFactory
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.Prefix

class ServerWiring(_port: Option[Int], prefix: Option[Prefix] = None, actorSystem: ActorSystem[SpawnProtocol.Command]) {
  private lazy val config = actorSystem.settings.config
  lazy val settings       = new Settings(_port, prefix, config, ComponentType.Service)

  private lazy val loggerFactory = new LoggerFactory(settings.httpConnection.prefix)
  lazy val logger: Logger        = loggerFactory.getLogger
  lazy val actorRuntime          = new ActorRuntime(actorSystem)
}

object ServerWiring {
  private[esw] def make(_port: Option[Int]): ServerWiring = {
    lazy val actorSystem: ActorSystem[SpawnProtocol.Command] =
      ActorSystemFactory.remote(SpawnProtocol(), "http-core-server-system")
    new ServerWiring(_port, actorSystem = actorSystem)
  }
}

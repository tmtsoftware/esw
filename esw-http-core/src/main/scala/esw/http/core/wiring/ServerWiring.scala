package esw.http.core.wiring

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import com.typesafe.config.ConfigFactory
import csw.location.client.ActorSystemFactory
import esw.http.core.utils.CswContext

class ServerWiring(_port: Option[Int]) {
  private lazy val config                          = ConfigFactory.load()
  lazy val settings                                = new Settings(_port, config)
  lazy val actorSystem: ActorSystem[SpawnProtocol] = ActorSystemFactory.remote(SpawnProtocol.behavior, "http-server")
  lazy val actorRuntime: ActorRuntime              = new ActorRuntime(actorSystem)
  lazy val cswCtx                                  = new CswContext(actorRuntime, settings.httpConnection, config)
}

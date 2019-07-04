package esw.template.http.server.wiring

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.client.ActorSystemFactory
import esw.template.http.server.csw.utils.CswContext

class ServerWiring(_port: Option[Int]) {
  lazy val settings                                = new Settings(_port)
  lazy val actorSystem: ActorSystem[SpawnProtocol] = ActorSystemFactory.remote(SpawnProtocol.behavior, "http-server")
  lazy val actorRuntime: ActorRuntime              = new ActorRuntime(actorSystem)

  lazy val cswCtx = new CswContext(actorRuntime, settings.httpConnection)
}

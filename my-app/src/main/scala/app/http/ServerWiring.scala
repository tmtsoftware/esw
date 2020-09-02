package app.http

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.server.Route
import csw.location.client.ActorSystemFactory
import csw.prefix.models.Prefix
import template.TemplateWiring

class ServerWiring(val port: Option[Int], val prefix: Option[Prefix]) extends TemplateWiring {
  lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "sample-app")
  lazy val serverImpl                                      = new ServerImpl
  lazy val routes: Route                                   = new ServerRoute(serverImpl, securityDirectives).route
}

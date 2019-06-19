package esw.gateway.server

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import esw.gateway.server.commons.ActorRuntime
import esw.gateway.server.http.{HttpService, Settings}

class Wiring(_port: Option[Int]) {
  lazy val settings                                = new Settings(_port)
  lazy val actorSystem: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "http-server")
  lazy val actorRuntime: ActorRuntime              = new ActorRuntime(actorSystem)
  lazy val locationService: LocationService =
    HttpLocationServiceFactory.makeLocalClient(actorSystem, actorRuntime.mat)

  lazy val routes = new Routes()

  lazy val httpService = new HttpService(locationService, routes.route, settings, actorRuntime)
}

package esw.gateway.server

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import com.typesafe.config.{Config, ConfigFactory}
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import esw.gateway.server.commons.ActorRuntime
import esw.gateway.server.http.HttpService

class Wiring(_port: Option[Int]) {
  lazy val config: Config                          = ConfigFactory.load().getConfig("esw-gateway-server")
  lazy val port: Int                               = _port.getOrElse(config.getInt("service-port"))
  lazy val actorSystem: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "esw-gateway-server")
  lazy val actorRuntime: ActorRuntime              = new ActorRuntime(actorSystem)
  lazy val locationService: LocationService =
    HttpLocationServiceFactory.makeLocalClient(actorSystem, actorRuntime.mat)

  lazy val routes = new Routes()

  lazy val httpService = new HttpService(locationService, routes.route, port, actorRuntime)
}

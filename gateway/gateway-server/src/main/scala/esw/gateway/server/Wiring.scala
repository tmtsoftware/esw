package esw.gateway.server

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.stream.typed.scaladsl.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.ExecutionContext

class Wiring(_port: Option[Int]) {
  lazy val config: Config = ConfigFactory.load().getConfig("esw-gateway-server")
  lazy val port: Int      = _port.getOrElse(config.getInt("service-port"))

  implicit lazy val actorSystem: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "esw-gateway-server")
  implicit lazy val materializer: ActorMaterializer         = ActorMaterializer()(actorSystem)
  implicit lazy val ec: ExecutionContext                    = actorSystem.executionContext

  lazy val routes = new Routes()

  lazy val server = new Server(routes, port)
}

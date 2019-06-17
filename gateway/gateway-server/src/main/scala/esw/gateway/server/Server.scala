package esw.gateway.server

import akka.actor
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.Http
import akka.stream.typed.scaladsl.ActorMaterializer

import scala.concurrent.Future

class Server(routes: Routes, port: Int)(implicit actorSystem: ActorSystem[SpawnProtocol], materializer: ActorMaterializer) {
  implicit lazy val untypedSystem: actor.ActorSystem = actorSystem.toUntyped

  def start: Future[Http.ServerBinding] = Http().bindAndHandle(routes.route, "0.0.0.0", port)
}

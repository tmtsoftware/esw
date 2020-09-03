package esw.http.template.wiring

import akka.Done
import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.typesafe.config.Config
import csw.aas.http.SecurityDirectives
import csw.location.api.models.{ComponentType, Metadata}
import csw.location.api.scaladsl.RegistrationResult
import csw.logging.api.scaladsl.Logger
import esw.http.core.wiring.{ActorRuntime, HttpService, Settings}

import scala.concurrent.Future

trait ServerWiring {
  val port: Option[Int]
  val actorSystem: ActorSystem[SpawnProtocol.Command]
  val routes: Route

  lazy val config: Config = actorSystem.settings.config
  lazy val settings       = new Settings(port, None, config, ComponentType.Service)
  lazy val actorRuntime   = new ActorRuntime(actorSystem)
  import actorRuntime.{ec, typedSystem}

  lazy val cswContext: CswContext = CswContext(settings.prefix)
  lazy val logger: Logger         = cswContext.loggerFactory.getLogger

  lazy val securityDirectives: SecurityDirectives = SecurityDirectives(config, cswContext.locationService)

  lazy val service = new HttpService(logger, cswContext.locationService, routes, settings, actorRuntime)

  def start(metadata: Metadata): Future[(Http.ServerBinding, RegistrationResult)] =
    service.startAndRegisterServer(metadata) // todo : fix metadata

  def stop(): Future[Done] = actorRuntime.shutdown(UnknownReason)
}

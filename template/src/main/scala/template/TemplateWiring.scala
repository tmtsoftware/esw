package template

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
import csw.prefix.models.Prefix
import esw.http.core.commons.ServiceLogger
import esw.http.core.wiring.{ActorRuntime, HttpService, Settings}

import scala.concurrent.Future

trait TemplateWiring {
  val port: Option[Int]
  val prefix: Option[Prefix]
  val actorSystem: ActorSystem[SpawnProtocol.Command]
  val routes: Route

  lazy val config: Config = actorSystem.settings.config
  lazy val settings       = new Settings(port, prefix, config, ComponentType.Service)
  lazy val logger: Logger = new ServiceLogger(settings.httpConnection).getLogger
  lazy val actorRuntime   = new ActorRuntime(actorSystem)
  import actorRuntime.{ec, typedSystem}

  lazy val cswWiring = new CswWiring()
  import cswWiring.locationService

  lazy val securityDirectives: SecurityDirectives = SecurityDirectives(config, locationService)

  lazy val service = new HttpService(logger, locationService, routes, settings, actorRuntime)

  def start(metadata: Metadata): Future[(Http.ServerBinding, RegistrationResult)] =
    service.startAndRegisterServer(metadata) // todo : fix metadata

  def stop(): Future[Done] = actorRuntime.shutdown(UnknownReason)
}

// csw dependencies version decided by us
// main class also coming from template

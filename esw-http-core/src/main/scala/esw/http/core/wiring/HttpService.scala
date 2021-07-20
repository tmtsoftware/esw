package esw.http.core.wiring

import akka.actor.CoordinatedShutdown
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives.handleRejections
import akka.http.scaladsl.server.{RejectionHandler, Route}
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{HttpRegistration, Metadata, NetworkType}
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.logging.api.scaladsl.Logger
import csw.network.utils.Networks
import esw.http.core.commons.CoordinatedShutdownReasons.FailureReason

import scala.async.Async._
import scala.concurrent.Future
import scala.util.control.NonFatal

/**
 * Initialises HTTP Server at given port and register with location service
 *
 * @param locationService locationService instance to be used for registering this server with the location service
 * @param route gateway server instance representing the routes supported by this server
 * @param settings application specific configurations
 * @param actorRuntime actorRuntime instance wrapper for actor system
 */
class HttpService(
    log: Logger,
    locationService: LocationService,
    route: Route,
    settings: Settings,
    actorRuntime: ActorRuntime,
    networkType: NetworkType = NetworkType.Outside
) {

  import actorRuntime._
  def startAndRegisterServer(metadata: Metadata = Metadata.empty): Future[(ServerBinding, RegistrationResult)] =
    async {
      val binding            = await(startServer())
      val registrationResult = await(register(binding, settings.httpConnection, metadata))

      coordinatedShutdown.addTask(
        CoordinatedShutdown.PhaseBeforeServiceUnbind,
        s"unregistering-${registrationResult.location}"
      )(() => registrationResult.unregister())

      log.info(s"Server online at http://${binding.localAddress.getHostString}:${binding.localAddress.getPort}/")
      (binding, registrationResult)
    } recoverWith { case NonFatal(ex) =>
      actorRuntime.shutdown(FailureReason(ex)).map(_ => throw ex)
    }

  private def applicationRoute: Route = {
    val rejectionHandler = corsRejectionHandler.withFallback(RejectionHandler.default)
    handleRejections(rejectionHandler) {
      cors() {
        handleRejections(rejectionHandler) {
          route
        }
      }
    }
  }

  private def startServer() = {
    val _host = Networks(networkType.envKey).hostname
    val _port = settings.port
    Http().newServerAt(_host, _port).bind(applicationRoute)
  }

  private def register(binding: ServerBinding, connection: HttpConnection, metadata: Metadata): Future[RegistrationResult] = {
    val registration = HttpRegistration(
      connection = connection,
      port = binding.localAddress.getPort,
      path = "",
      networkType,
      metadata
    )

    log.info(
      s"Registering ${connection.name} Service HTTP Server with Location Service using registration: [${registration.toString}]"
    )
    val eventualResult = locationService.register(registration)
    eventualResult
  }
}

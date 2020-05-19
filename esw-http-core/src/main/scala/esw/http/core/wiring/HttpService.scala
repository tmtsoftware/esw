package esw.http.core.wiring

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.Reason
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives.handleRejections
import akka.http.scaladsl.server.{RejectionHandler, Route}
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{HttpRegistration, NetworkType}
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.logging.api.scaladsl.Logger
import csw.network.utils.Networks
import esw.http.core.commons.CoordinatedShutdownReasons.FailureReason

import scala.async.Async._
import scala.concurrent.Future
import scala.concurrent.duration.DurationLong
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
    actorRuntime: ActorRuntime
) {

  import actorRuntime._
  lazy val registeredLazyBinding: Future[(ServerBinding, RegistrationResult)] = async {
    val binding = await(bind()) // create HttpBinding with appropriate hostname and port
    val registrationResult =
      await(register(binding, settings.httpConnection)) // create HttpRegistration and register it with location service

    // Add the task to unregister the HttpRegistration from location service.
    // This will execute as the first task out of all tasks at the shutdown of ActorSystem.
    // ActorSystem will shutdown if any SVNException is thrown while running the ConfigService app (refer Main.scala)
    // If for some reason ActorSystem is not shutdown gracefully then a jvm shutdown hook is in place which will unregister
    // HttpRegistration from location service.
    // And if for some reason jvm shutdown hook does not get executed then the DeathWatchActor running in cluster will get notified that config service is down
    // and it will unregister HttpRegistration from location service.
    coordinatedShutdown.addTask(
      CoordinatedShutdown.PhaseBeforeServiceUnbind,
      s"unregistering-${registrationResult.location}"
    )(() => registrationResult.unregister())

    log.info(s"Server online at http://${binding.localAddress.getHostName}:${binding.localAddress.getPort}/")
    (binding, registrationResult)
  } recoverWith {
    case NonFatal(ex) => actorRuntime.shutdown(FailureReason(ex)).map(_ => throw ex)
  }

  def shutdown(reason: Reason): Future[Done] = {
    val httpTerminatedF = registeredLazyBinding.flatMap(_._1.terminate(10.seconds))
    httpTerminatedF.flatMap(_ => actorRuntime.shutdown(reason))
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

  private def bind() = {
    val _host = Networks(NetworkType.Public.envKey).hostname
    val _port = settings.port

    Http().bindAndHandle(
      handler = applicationRoute,
      interface = _host,
      port = _port
    )
  }

  private def register(binding: ServerBinding, connection: HttpConnection): Future[RegistrationResult] = {
    val registration = HttpRegistration(
      connection = connection,
      port = binding.localAddress.getPort,
      path = "",
      NetworkType.Public
    )

    log.info(
      s"Registering ${connection.name} Service HTTP Server with Location Service using registration: [${registration.toString}]"
    )
    locationService.register(registration)
  }
}

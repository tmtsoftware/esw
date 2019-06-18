package esw.gateway.server.http

import java.net.BindException

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.Reason
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Route
import csw.location.api.models.{HttpRegistration, RegistrationResult}
import csw.location.api.scaladsl.LocationService
import csw.network.utils.{Networks, SocketUtils}
import esw.gateway.server.commons.ActorRuntime
import esw.gateway.server.commons.CoordinatedShutdownReasons.FailureReason

import scala.async.Async._
import scala.concurrent.Future
import scala.util.control.NonFatal

/**
 * Initialises Gateway Server at given port and register with location service
 *
 * @param locationService locationService instance to be used for registering this server with the location service
 * @param route gateway server instance representing the routes supported by this server
 * @param port port of server
 * @param actorRuntime actorRuntime instance wrapper for actor system
 */
class HttpService(
    locationService: LocationService,
    route: Route,
    port: Int,
    actorRuntime: ActorRuntime
) {

  import actorRuntime._
  lazy val registeredLazyBinding: Future[(ServerBinding, RegistrationResult)] = async {
    val binding            = await(bind())            // create HttpBinding with appropriate hostname and port
    val registrationResult = await(register(binding)) // create HttpRegistration and register it with location service

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

    (binding, registrationResult)
  } recoverWith {
    case NonFatal(ex) ⇒ shutdown(FailureReason(ex)).map(_ ⇒ throw ex)
  }

  def shutdown(reason: Reason): Future[Done] = actorRuntime.shutdown(reason)

  private def bind() = {
    val _host = Networks().hostname
    val _port = port

    /*
      Explicitly check _host:_port is free as we register Networks().hostname with location service
      But we bind server to all interfaces so that it can be accessible from any server via localhost
      and it can happen that server gets bind to some of the interfaces and not to Networks().hostname
     */
    if (SocketUtils.isAddressInUse(_host, _port))
      throw new BindException(s"Bind failed for TCP channel on endpoint [${_host}:${_port}]. Address already in use.")

    Http().bindAndHandle(
      handler = route,
      interface = "0.0.0.0",
      port = _port
    )
  }

  private def register(binding: ServerBinding): Future[RegistrationResult] = {
    val registration = HttpRegistration(
      connection = GatewayConnection.value,
      port = binding.localAddress.getPort,
      path = ""
    )

    locationService.register(registration)
  }
}

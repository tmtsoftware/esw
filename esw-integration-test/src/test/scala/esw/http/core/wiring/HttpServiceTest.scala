package esw.http.core.wiring

import java.net.BindException

import akka.actor.CoordinatedShutdown.UnknownReason
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.{complete, get, path}
import akka.http.scaladsl.server.Route
import csw.location.api.exceptions.OtherLocationIsRegistered
import csw.location.api.models.HttpRegistration
import csw.network.utils.{Networks, SocketUtils}
import esw.ocs.testkit.EswTestKit

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.control.NonFatal

class HttpServiceTest extends EswTestKit {

  private val route: Route =
    path("hello") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
      }
    }

  "HttpService" must {
    "start the http server and register with location service | ESW-86" in {
      val _servicePort = 4005

      val wiring = new ServerWiring(Some(_servicePort))
      import wiring._
      import wiring.cswWiring.actorRuntime

      val httpService             = new HttpService(logger, locationService, route, settings, actorRuntime)
      val (_, registrationResult) = Await.result(httpService.registeredLazyBinding, 5.seconds)

      Await.result(locationService.find(settings.httpConnection), 5.seconds).get.connection shouldBe settings.httpConnection

      val location = registrationResult.location
      location.uri.getHost shouldBe Networks().hostname
      location.connection shouldBe settings.httpConnection
      Await.result(actorRuntime.shutdown(UnknownReason), 5.seconds)
    }

    "not register with location service if server binding fails | ESW-86" in {
      val _servicePort = 4452 // Location Service runs on this port
      val wiring       = new ServerWiring(Some(_servicePort))
      import wiring._
      import wiring.cswWiring.actorRuntime

      val httpService = new HttpService(logger, locationService, route, settings, actorRuntime)

      a[BindException] shouldBe thrownBy(Await.result(httpService.registeredLazyBinding, 5.seconds))
      Await.result(locationService.find(settings.httpConnection), 5.seconds) shouldBe None
    }

    "not start server if registration with location service fails | ESW-86" in {
      val _servicePort = 4007
      val wiring       = new ServerWiring(Some(_servicePort))
      import wiring._
      import wiring.cswWiring.actorRuntime

      val httpService = new HttpService(logger, locationService, route, settings, actorRuntime)

      Await.result(locationService.register(HttpRegistration(settings.httpConnection, 21212, "")), 5.seconds)
      Await.result(locationService.find(settings.httpConnection), 5.seconds).get.connection shouldBe settings.httpConnection

      a[OtherLocationIsRegistered] shouldBe thrownBy(Await.result(httpService.registeredLazyBinding, 5.seconds))

      SocketUtils.isAddressInUse("localhost", 4007) shouldEqual false

      try Await.result(actorRuntime.shutdown(UnknownReason), 5.seconds)
      catch {
        case NonFatal(_) =>
      }
    }
  }
}

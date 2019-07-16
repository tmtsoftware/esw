package esw.integration.test.template.http.server

import java.net.BindException

import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.{complete, get, path}
import akka.http.scaladsl.server.Route
import akka.stream.typed.scaladsl.ActorMaterializer
import csw.location.api.exceptions.OtherLocationIsRegistered
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.model.scaladsl.HttpRegistration
import csw.network.utils.Networks
import csw.testkit.LocationTestKit
import esw.template.http.server.BaseTestSuite
import esw.template.http.server.wiring.{HttpService, ServerWiring}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.control.NonFatal

class HttpServiceTest extends BaseTestSuite {

  private val testKit                              = LocationTestKit()
  implicit val system: ActorSystem[_]              = ActorSystem(Behaviors.empty, "test")
  implicit val mat: ActorMaterializer              = ActorMaterializer()
  private val testLocationService: LocationService = HttpLocationServiceFactory.makeLocalClient
  private val route: Route = {
    path("hello") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
      }
    }
  }

  override def beforeAll(): Unit = {
    testKit.startLocationServer()
  }

  override def afterAll(): Unit = {
    testKit.shutdownLocationServer()
    system.terminate
    Await.result(system.whenTerminated, 10.seconds)
    super.afterAll()
  }

  "HttpService" must {
    "start the http server and register with location service | ESW-86" in {
      val _servicePort = 4005

      val wiring = new ServerWiring(Some(_servicePort))
      import wiring._
      import wiring.cswCtx._

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
      import wiring.cswCtx._

      val httpService = new HttpService(logger, locationService, route, settings, actorRuntime)

      a[BindException] shouldBe thrownBy(Await.result(httpService.registeredLazyBinding, 5.seconds))
      Await.result(testLocationService.find(settings.httpConnection), 5.seconds) shouldBe None
    }

    "not start server if registration with location service fails | ESW-86" in {
      val _servicePort = 4007
      val wiring       = new ServerWiring(Some(_servicePort))
      import wiring._
      import wiring.cswCtx._

      val httpService = new HttpService(logger, locationService, route, settings, actorRuntime)

      Await.result(locationService.register(HttpRegistration(settings.httpConnection, 21212, "")), 5.seconds)
      Await.result(locationService.find(settings.httpConnection), 5.seconds).get.connection shouldBe settings.httpConnection

      a[OtherLocationIsRegistered] shouldBe thrownBy(Await.result(httpService.registeredLazyBinding, 5.seconds))

      //TODO: Find a way to assert server is not bounded
      try Await.result(actorRuntime.shutdown(UnknownReason), 5.seconds)
      catch {
        case NonFatal(_) =>
      }
    }
  }
}

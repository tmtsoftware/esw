package esw.template.http.server.http

import java.net.BindException

import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.{complete, get, path}
import akka.http.scaladsl.server.Route
import akka.stream.typed.scaladsl.ActorMaterializer
import csw.location.api.exceptions.OtherLocationIsRegistered
import csw.location.api.models.HttpRegistration
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.network.utils.Networks
import csw.testkit.LocationTestKit
import esw.template.http.server.{BaseTestSuit, CswContext}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.control.NonFatal

class HttpServiceTest extends BaseTestSuit {

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

  test("ESW-86 | should start the http server and register with location service") {
    val _servicePort = 4005
    val cswContext   = new CswContext(Some(_servicePort))
    import cswContext._
    val httpService             = new HttpService(locationService, route, settings, actorRuntime)
    val (_, registrationResult) = Await.result(httpService.registeredLazyBinding, 5.seconds)

    Await.result(locationService.find(settings.httpConection), 5.seconds).get.connection shouldBe settings.httpConection

    val location = registrationResult.location
    location.uri.getHost shouldBe Networks().hostname
    location.connection shouldBe settings.httpConection
    Await.result(actorRuntime.shutdown(UnknownReason), 5.seconds)
  }

  test("ESW-86 | should not register with location service if server binding fails") {
    val _servicePort = 4452 // Location Service runs on this port
    val cswContext   = new CswContext(Some(_servicePort))
    import cswContext._
    val httpService             = new HttpService(locationService, route, settings, actorRuntime)
    val (_, registrationResult) = Await.result(httpService.registeredLazyBinding, 5.seconds)

    a[BindException] shouldBe thrownBy(Await.result(httpService.registeredLazyBinding, 5.seconds))
    Await.result(testLocationService.find(settings.httpConection), 5.seconds) shouldBe None
  }

  test("ESW-86 | should not start server if registration with location service fails") {
    val _servicePort = 4007
    val cswContext   = new CswContext(Some(_servicePort))
    import cswContext._
    val httpService             = new HttpService(locationService, route, settings, actorRuntime)
    val (_, registrationResult) = Await.result(httpService.registeredLazyBinding, 5.seconds)

    Await.result(locationService.register(HttpRegistration(settings.httpConection, 21212, "")), 5.seconds)
    Await.result(locationService.find(settings.httpConection), 5.seconds).get.connection shouldBe settings.httpConection

    a[OtherLocationIsRegistered] shouldBe thrownBy(Await.result(httpService.registeredLazyBinding, 5.seconds))

    //TODO: Find a way to assert server is not bounded
    try Await.result(actorRuntime.shutdown(UnknownReason), 5.seconds)
    catch {
      case NonFatal(_) ⇒
    }
  }
}

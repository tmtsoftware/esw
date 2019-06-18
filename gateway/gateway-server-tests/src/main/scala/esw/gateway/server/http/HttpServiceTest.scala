package esw.gateway.server.http

import java.net.BindException

import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.typed.scaladsl.ActorMaterializer
import csw.location.api.exceptions.OtherLocationIsRegistered
import csw.location.api.models.HttpRegistration
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.network.utils.Networks
import csw.testkit.LocationTestKit
import esw.gateway.server.Wiring
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.control.NonFatal

class HttpServiceTest extends FunSuite with Matchers with BeforeAndAfterAll {

  private val testKit                              = LocationTestKit()
  implicit val system: ActorSystem[_]              = ActorSystem(Behaviors.empty, "test")
  implicit val mat: ActorMaterializer              = ActorMaterializer()
  private val testLocationService: LocationService = HttpLocationServiceFactory.makeLocalClient

  override def beforeAll(): Unit = {
    testKit.startLocationServer()
  }

  override def afterAll(): Unit = {
    testKit.shutdownLocationServer()
  }

  test("ESW-86 | should start the http server and register with location service") {
    val _servicePort = 4005
    val wiring       = new Wiring(Some(_servicePort))
    import wiring._
    val (_, registrationResult) = Await.result(httpService.registeredLazyBinding, 5.seconds)

    Await.result(locationService.find(GatewayConnection.value), 5.seconds).get.connection shouldBe GatewayConnection.value

    val location = registrationResult.location
    location.uri.getHost shouldBe Networks().hostname
    location.connection shouldBe GatewayConnection.value
    Await.result(actorRuntime.shutdown(UnknownReason), 5.seconds)
  }

  test("ESW-86 | should not register with location service if server binding fails") {
    val _servicePort = 4452 // Location Service runs on this port
    val wiring       = new Wiring(Some(_servicePort))

    import wiring._

    a[BindException] shouldBe thrownBy(Await.result(httpService.registeredLazyBinding, 5.seconds))
    Await.result(testLocationService.find(GatewayConnection.value), 5.seconds) shouldBe None
  }

  test("ESW-86 | should not start server if registration with location service fails") {
    val _servicePort = 4007
    val wiring       = new Wiring(Some(_servicePort))
    import wiring._
    Await.result(locationService.register(HttpRegistration(GatewayConnection.value, 21212, "")), 5.seconds)
    Await.result(locationService.find(GatewayConnection.value), 5.seconds).get.connection shouldBe GatewayConnection.value

    a[OtherLocationIsRegistered] shouldBe thrownBy(Await.result(httpService.registeredLazyBinding, 5.seconds))

    //TODO: Find a way to assert server is not bounded
    try Await.result(actorRuntime.shutdown(UnknownReason), 5.seconds)
    catch {
      case NonFatal(_) ⇒
    }
  }
}

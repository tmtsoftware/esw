package esw.integration.test.gatway.server

import akka.actor
import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.model.{HttpRequest, StatusCodes, Uri}
import akka.stream.typed.scaladsl.ActorMaterializer
import com.typesafe.config.ConfigFactory
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{ComponentId, ComponentType}
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.testkit.LocationTestKit
import esw.gateway.server.Main
import esw.template.http.server.BaseTestSuite
import esw.template.http.server.TestFutureExtensions.RichFuture

import scala.concurrent.duration.DurationInt

class MainTest extends BaseTestSuite {
  private val testKit = LocationTestKit()

  implicit val system: ActorSystem[_]                = ActorSystem(Behaviors.empty, "test")
  implicit val untypedActorSystem: actor.ActorSystem = system.toUntyped
  implicit val mat: ActorMaterializer                = ActorMaterializer()
  private val testLocationService: LocationService   = HttpLocationServiceFactory.makeLocalClient

  override def beforeAll(): Unit = {
    testKit.startLocationServer()
  }

  override def afterAll(): Unit = {
    Http().shutdownAllConnectionPools().await
    testKit.shutdownLocationServer()
    system.terminate()
    system.whenTerminated.await
  }

  "should start Gateway server and register with location service | ESW-92" in {
    val httpService = Main.start(Array("--port", "9806"), startLogging = false).get
    val connection  = HttpConnection(ComponentId("GatewayServer", ComponentType.Service))
    val expectedConnection = HttpConnection(
      ComponentId(ConfigFactory.load().getConfig("http-server").getString("connection-name"), ComponentType.Service)
    )

    try {
      val gatewayServiceLocation = testLocationService.resolve(connection, 5.seconds).await.get

      gatewayServiceLocation.connection shouldBe expectedConnection
      val uri = Uri(gatewayServiceLocation.uri.toString).withPath(Path / "event").withQuery(Query(("key", "tcs.test.gateway")))

      val request  = HttpRequest(uri = uri)
      val response = Http().singleRequest(request).await
      response.status shouldBe StatusCodes.InternalServerError
    } finally {
      httpService.shutdown(UnknownReason).await
    }
  }
}

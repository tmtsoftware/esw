package esw.integration.test.gatway.server

import akka.actor
import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model._
import akka.stream.typed.scaladsl.ActorMaterializer
import com.typesafe.config.ConfigFactory
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{ComponentId, ComponentType}
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.core.formats.JsonSupport
import csw.params.core.models.Prefix
import csw.params.events.{EventName, SystemEvent}
import csw.testkit.{EventTestKit, LocationTestKit}
import esw.gateway.server.Main
import esw.template.http.server.BaseTestSuite
import esw.template.http.server.TestFutureExtensions.RichFuture

import scala.concurrent.duration.DurationInt

class MainTest extends BaseTestSuite {

  import JsonSupport._

  private val locationTestKit = LocationTestKit()
  private val eventTestKit    = EventTestKit()

  implicit val system: ActorSystem[_]                = ActorSystem(Behaviors.empty, "test")
  implicit val untypedActorSystem: actor.ActorSystem = system.toUntyped
  implicit val mat: ActorMaterializer                = ActorMaterializer()
  private val testLocationService: LocationService   = HttpLocationServiceFactory.makeLocalClient

  override def beforeAll(): Unit = {
    locationTestKit.startLocationServer()
    eventTestKit.startEventService()
  }

  override def afterAll(): Unit = {
    Http().shutdownAllConnectionPools().await
    locationTestKit.shutdownLocationServer()
    eventTestKit.shutdown()
    system.terminate()
    system.whenTerminated.await
  }

  "should start Gateway server and register with location service and publish event | ESW-92" in {
    val httpService = Main.start(Array("--port", "9806"), startLogging = false).get
    val connection  = HttpConnection(ComponentId("GatewayServer", ComponentType.Service))
    val expectedConnection = HttpConnection(
      ComponentId(ConfigFactory.load().getConfig("http-server").getString("connection-name"), ComponentType.Service)
    )

    try {
      val gatewayServiceLocation = testLocationService.resolve(connection, 5.seconds).await.get

      gatewayServiceLocation.connection shouldBe expectedConnection
      val uri       = Uri(gatewayServiceLocation.uri.toString).withPath(Path / "event")
      val event     = SystemEvent(Prefix("tcs.test.gateway"), EventName("event"))
      val eventJson = HttpEntity(ContentTypes.`application/json`, writeEvent(event).toString())

      val request  = HttpRequest(uri = uri).withMethod(HttpMethods.POST).withEntity(eventJson)
      val response = Http().singleRequest(request).await
      response.status shouldBe StatusCodes.OK

    } finally {
      httpService.shutdown(UnknownReason).await
    }
  }
}

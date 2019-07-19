package esw.gatway.server

import akka.actor
import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.typed.scaladsl.ActorMaterializer
import com.typesafe.config.ConfigFactory
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.model.scaladsl.Connection.HttpConnection
import csw.location.model.scaladsl.{ComponentId, ComponentType}
import csw.params.core.formats.JsonSupport
import csw.params.core.generics.Parameter
import csw.params.core.models.Prefix
import csw.params.events.{Event, EventName, SystemEvent}
import csw.testkit.{EventTestKit, LocationTestKit}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import esw.gateway.server.Main
import esw.http.core.BaseTestSuite
import esw.http.core.TestFutureExtensions.RichFuture

import scala.concurrent.duration.DurationInt

class MainTest extends BaseTestSuite with JsonSupport with PlayJsonSupport {

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
    val httpService = Main.start(Some(9806), startLogging = false)
    val connection  = HttpConnection(ComponentId("EswGateway", ComponentType.Service))
    val expectedConnection = HttpConnection(
      ComponentId(ConfigFactory.load().getConfig("http-server").getString("service-name"), ComponentType.Service)
    )

    try {
      val gatewayServiceLocation = testLocationService.resolve(connection, 5.seconds).await.get

      gatewayServiceLocation.connection shouldBe expectedConnection
      val uri       = Uri(gatewayServiceLocation.uri.toString).withPath(Path / "event")
      val event     = SystemEvent(Prefix("tcs.test.gateway"), EventName("event"), Set.empty[Parameter[_]])
      val jsObject  = eventFormat.writes(event).toString()
      val eventJson = HttpEntity(ContentTypes.`application/json`, jsObject)

      //Publish event
      val request  = HttpRequest(uri = uri, method = HttpMethods.POST, entity = eventJson)
      val response = Http().singleRequest(request).await

      //assert if event is successfully published
      response.status shouldBe StatusCodes.OK

      //Get event by specifying event key
      val getRequest  = HttpRequest(uri = uri.withQuery(Query(("key", "tcs.test.gateway.event"))))
      val getResponse = Http().singleRequest(getRequest).await

      //assert on response of getEvent call
      getResponse.status shouldBe StatusCodes.OK
      val actualEvent = Unmarshal(getResponse.entity).to[Set[Event]].await
      actualEvent shouldEqual Set(event)

    } finally {
      httpService.shutdown(UnknownReason).await
    }
  }
}

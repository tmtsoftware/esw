package esw.gateway.server2

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.typed.scaladsl.ActorMaterializer
import akka.{Done, actor}
import csw.params.core.generics.KeyType
import csw.params.core.models.{ArrayData, Prefix, Subsystem}
import csw.params.events.{Event, EventKey, EventName, SystemEvent}
import csw.testkit.{EventTestKit, LocationTestKit}
import esw.gateway.api.clients.EventClient
import esw.gateway.api.codecs.RestlessCodecs
import esw.gateway.api.messages.{EmptyEventKeys, PostRequest, WebsocketRequest}
import esw.http.core.BaseTestSuite
import esw.http.core.commons.CoordinatedShutdownReasons
import mscoket.impl.{PostClientJvm, WebsocketClientJvm}
import msocket.api.RequestClient

import scala.concurrent.duration.{DurationInt, FiniteDuration}

class EventGatewayTest extends BaseTestSuite with RestlessCodecs {
  private val locationTestKit            = LocationTestKit()
  private val eventTestKit: EventTestKit = EventTestKit()

  implicit val system: ActorSystem[_]                  = ActorSystem(Behaviors.empty, "test")
  implicit val untypedActorSystem: actor.ActorSystem   = system.toUntyped
  implicit val mat: ActorMaterializer                  = ActorMaterializer()
  implicit val timeout: FiniteDuration                 = 10.seconds
  override implicit def patienceConfig: PatienceConfig = PatienceConfig(timeout)
  var port: Int                                        = _
  var gatewayWiring: GatewayWiring                     = _

  //Event
  private val a1: Array[Int] = Array(1, 2, 3, 4, 5)
  private val a2: Array[Int] = Array(10, 20, 30, 40, 50)

  private val arrayDataKey   = KeyType.IntArrayKey.make("arrayDataKey")
  private val arrayDataParam = arrayDataKey.set(ArrayData(a1), ArrayData(a2))
  private val prefix         = Prefix("tcs.test.gateway")
  private val name1          = EventName("event1")
  private val name2          = EventName("event2")
  private val event1: Event  = SystemEvent(prefix, name1, Set(arrayDataParam))
  private val event2: Event  = SystemEvent(prefix, name2, Set(arrayDataParam))
  private val eventKeys      = Set(EventKey(prefix, name1), EventKey(prefix, name2))

  override def beforeAll(): Unit = {
    locationTestKit.startLocationServer()
  }

  override def beforeEach(): Unit = {
    eventTestKit.startEventService()
    port = 6490
    gatewayWiring = new GatewayWiring(Some(port))
    gatewayWiring.httpService.registeredLazyBinding.futureValue
  }

  override def afterEach(): Unit = {
    eventTestKit.stopRedis()
    gatewayWiring.httpService.shutdown(CoordinatedShutdownReasons.ApplicationFinishedReason).futureValue
  }

  override def afterAll(): Unit = {
    eventTestKit.shutdown()
    locationTestKit.shutdownLocationServer()
    system.terminate()
    system.whenTerminated.futureValue
  }

  "EventApi" must {
    "publish, get, subscribe and pattern subscribe events | ESW-216" in {
      val postClient: RequestClient[PostRequest] = new PostClientJvm[PostRequest](s"http://localhost:$port/post")
      val websocketClient: RequestClient[WebsocketRequest] =
        new WebsocketClientJvm[WebsocketRequest](s"ws://localhost:$port/websocket")
      val eventClient: EventClient = new EventClient(postClient, websocketClient)

      val eventsF  = eventClient.subscribe(eventKeys, None).take(4).runWith(Sink.seq)
      val pEventsF = eventClient.pSubscribe(Subsystem.TCS, None, "*").take(2).runWith(Sink.seq)
      Thread.sleep(500)

      //publish event successfully
      eventClient.publish(event1).futureValue should ===(Done)
      eventClient.publish(event2).futureValue should ===(Done)

      //get set of events
      eventClient.get(Set(EventKey(prefix, name1))).rightValue should ===(Set(event1))

      //subscribe events returns a set of events successfully
      val invalidEvent1 = Event.invalidEvent(EventKey(prefix, name1))
      val invalidEvent2 = Event.invalidEvent(EventKey(prefix, name2))
      eventsF.futureValue.toSet should ===(Set(invalidEvent1, invalidEvent2, event1, event2))

      //pSubscribe events returns a set of events successfully
      pEventsF.futureValue.toSet should ===(Set(event1, event2))

    }

    "subscribe events returns an EmptyEventKeys error on sending no event keys in subscription| ESW-216" in {
      val postClient: RequestClient[PostRequest] = new PostClientJvm[PostRequest](s"http://localhost:$port/post")
      val websocketClient: RequestClient[WebsocketRequest] =
        new WebsocketClientJvm[WebsocketRequest](s"ws://localhost:$port/websocket")
      val eventClient: EventClient = new EventClient(postClient, websocketClient)

      eventClient.subscribe(Set.empty, None).toMat(Sink.head)(Keep.left).run().futureValue.get should ===(EmptyEventKeys())
    }

  }

}

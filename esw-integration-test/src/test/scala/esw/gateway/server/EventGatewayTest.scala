package esw.gateway.server

import akka.Done
import akka.actor.CoordinatedShutdown.UnknownReason
import akka.stream.scaladsl.Sink
import csw.params.core.generics.KeyType.{ByteKey, StructKey}
import csw.params.core.generics.{KeyType, Parameter}
import csw.params.core.models._
import csw.params.events.{Event, EventKey, EventName, SystemEvent}
import csw.testkit.scaladsl.CSWService.EventServer
import esw.gateway.api.clients.EventClient
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.{PostRequest, WebsocketRequest}
import esw.ocs.testkit.EswTestKit
import msocket.api.Transport
import msocket.api.models.ServiceException
import msocket.impl.Encoding.JsonText
import msocket.impl.post.HttpPostTransport
import msocket.impl.ws.WebsocketTransport

class EventGatewayTest extends EswTestKit(EventServer) with GatewayCodecs {

  private val port: Int                    = 6490
  private val gatewayWiring: GatewayWiring = new GatewayWiring(Some(port))

  //Event
  private val a1: Array[Int] = Array(1, 2, 3, 4, 5)
  private val a2: Array[Int] = Array(10, 20, 30, 40, 50)

  private val arrayDataKey   = KeyType.IntArrayKey.make("arrayDataKey")
  private val arrayDataParam = arrayDataKey.set(ArrayData(a1), ArrayData(a2))
  private val byteKey        = ByteKey.make("byteKey")
  private val structKey      = StructKey.make("structKey")
  private val paramSet: Set[Parameter[_]] = Set(
    byteKey.set(100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100)
  )
  val largeData: Parameter[Struct] = structKey.set((1 to 10000).map(_ => Struct(paramSet)): _*)

  private val prefix        = Prefix("tcs.test.gateway")
  private val name1         = EventName("event1")
  private val name2         = EventName("event2")
  private val name3         = EventName("event3")
  private val event1        = SystemEvent(prefix, name1, Set(arrayDataParam))
  private val event2        = SystemEvent(prefix, name2, Set(arrayDataParam))
  private val largeEvent    = SystemEvent(prefix, name3, paramSet)
  private val invalidEvent1 = Event.invalidEvent(EventKey(prefix, name1))
  private val invalidEvent2 = Event.invalidEvent(EventKey(prefix, name2))
  private val invalidEvent3 = Event.invalidEvent(EventKey(prefix, name3))
  private val eventKeys     = Set(EventKey(prefix, name1), EventKey(prefix, name2))

  override def beforeAll(): Unit = {
    super.beforeAll()
    gatewayWiring.httpService.registeredLazyBinding.futureValue
  }

  override def afterAll(): Unit = {
    gatewayWiring.httpService.shutdown(UnknownReason).futureValue
    super.afterAll()
  }

  "EventApi" must {
    "publish, get, subscribe and pattern subscribe events | ESW-94, ESW-93, ESW-92, ESW-216, ESW-86" in {
      val postClient: Transport[PostRequest] =
        new HttpPostTransport[PostRequest](s"http://localhost:$port/post-endpoint", JsonText, () => None)
      val websocketClient: Transport[WebsocketRequest] =
        new WebsocketTransport[WebsocketRequest](s"ws://localhost:$port/websocket-endpoint", JsonText)
      val eventClient: EventClient = new EventClient(postClient, websocketClient)

      val eventsF  = eventClient.subscribe(eventKeys, None).take(4).runWith(Sink.seq)
      val pEventsF = eventClient.pSubscribe(Subsystem.TCS, None).take(2).runWith(Sink.seq)
      Thread.sleep(500)

      //publish event successfully
      eventClient.publish(event1).futureValue should ===(Right(Done))
      eventClient.publish(event2).futureValue should ===(Right(Done))

      //get set of events
      eventClient.get(Set(EventKey(prefix, name1))).rightValue shouldBe Set(event1)

      // get returns invalid event for event that hasn't been published
      val name4 = EventName("event4")
      eventClient.get(Set(EventKey(prefix, name4))).rightValue shouldBe Set(Event.invalidEvent(EventKey(prefix, name4)))

      //subscribe events returns a set of events successfully
      eventsF.futureValue.toSet shouldBe Set(invalidEvent1, invalidEvent2, event1, event2)

      //pSubscribe events returns a set of events successfully
      pEventsF.futureValue.toSet shouldBe Set(event1, event2)

    }

    "subscribe events returns an EmptyEventKeys error on sending no event keys in subscription| ESW-93, ESW-216, ESW-86" in {
      val postClient: Transport[PostRequest] =
        new HttpPostTransport[PostRequest](s"http://localhost:$port/post-endpoint", JsonText, () => None)
      val websocketClient: Transport[WebsocketRequest] =
        new WebsocketTransport[WebsocketRequest](s"ws://localhost:$port/websocket-endpoint", JsonText)
      val eventClient: EventClient = new EventClient(postClient, websocketClient)

      intercept[ServiceException] {
        eventClient.subscribe(Set.empty, None).runForeach(_ => ()).awaitResult
      }
    }

    "support pubsub of large events" in {
      val postClient: Transport[PostRequest] =
        new HttpPostTransport[PostRequest](s"http://localhost:$port/post-endpoint", JsonText, () => None)
      val websocketClient: Transport[WebsocketRequest] =
        new WebsocketTransport[WebsocketRequest](s"ws://localhost:$port/websocket-endpoint", JsonText)
      val eventClient: EventClient = new EventClient(postClient, websocketClient)

      val eventsF = eventClient.subscribe(Set(largeEvent.eventKey), None).take(2).runWith(Sink.seq)
      Thread.sleep(500)

      eventClient.publish(largeEvent).futureValue should ===(Right(Done))
      eventsF.futureValue.toSet shouldBe Set(invalidEvent3, largeEvent)
    }
  }
}

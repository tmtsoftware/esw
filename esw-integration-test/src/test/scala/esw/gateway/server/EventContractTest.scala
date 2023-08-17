package esw.gateway.server

import org.apache.pekko.Done
import org.apache.pekko.stream.scaladsl.Sink
import csw.params.core.generics.KeyType.ByteKey
import csw.params.core.generics.{KeyType, Parameter}
import csw.params.core.models.*
import csw.params.events.*
import csw.prefix.models.{Prefix, Subsystem}
import csw.testkit.scaladsl.CSWService.EventServer
import esw.gateway.api.clients.EventClient
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.GatewayException
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.Gateway

class EventContractTest extends EswTestKit(EventServer, Gateway) with GatewayCodecs {

  // Event
  private val a1: Array[Int] = Array(1, 2, 3, 4, 5)
  private val a2: Array[Int] = Array(10, 20, 30, 40, 50)

  private val arrayDataKey   = KeyType.IntArrayKey.make("arrayDataKey")
  private val arrayDataParam = arrayDataKey.set(ArrayData(a1), ArrayData(a2))
  private val byteKey        = ByteKey.make("byteKey")
  private val paramSet: Set[Parameter[_]] = Set(
    byteKey.set(100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100)
  )

  private val prefix        = Prefix("tcs.test.gateway")
  private val name1         = EventName("event1")
  private val name2         = EventName("event2")
  private val name3         = EventName("event3")
  private val event1        = SystemEvent(prefix, name1, Set(arrayDataParam))
  private val event2        = SystemEvent(prefix, name2, Set(arrayDataParam))
  private val obsEvent1     = IRDetectorEvent.observeStart(prefix)
  private val largeEvent    = SystemEvent(prefix, name3, paramSet)
  private val invalidEvent1 = Event.invalidEvent(EventKey(prefix, name1))
  private val invalidEvent2 = Event.invalidEvent(EventKey(prefix, name2))
  private val invalidEvent3 = Event.invalidEvent(EventKey(prefix, name3))
  private val eventKeys     = Set(EventKey(prefix, name1), EventKey(prefix, name2))

  "EventApi" must {
    "publish, get, subscribe and pattern subscribe events | ESW-94, ESW-93, ESW-92, ESW-216, ESW-86, ESW-581" in {
      val eventClient: EventClient = new EventClient(gatewayPostClient, gatewayWsClient)

      val eventsF        = eventClient.subscribe(eventKeys, None).take(4).runWith(Sink.seq)
      val pEventsF       = eventClient.pSubscribe(Subsystem.TCS, None).take(2).runWith(Sink.seq)
      val observeEventsF = eventClient.subscribeObserveEvents().take(1).runWith(Sink.seq)
      Thread.sleep(500)

      // publish event successfully
      eventClient.publish(event1).futureValue should ===(Done)
      eventClient.publish(event2).futureValue should ===(Done)
      eventClient.publish(obsEvent1).futureValue should ===(Done)

      // get set of events
      eventClient.get(Set(EventKey(prefix, name1))).futureValue shouldBe Set(event1)

      // get returns invalid event for event that hasn't been published
      val name4 = EventName("event4")
      eventClient.get(Set(EventKey(prefix, name4))).futureValue shouldBe Set(Event.invalidEvent(EventKey(prefix, name4)))

      // subscribe events returns a set of events successfully
      eventsF.futureValue.toSet shouldBe Set(invalidEvent1, invalidEvent2, event1, event2)

      // pSubscribe events returns a set of events successfully
      pEventsF.futureValue.toSet shouldBe Set(event1, event2)

      // subscribeObserveEvents returns all the observe events
      observeEventsF.futureValue.toSet shouldBe Set(obsEvent1)
    }

    "subscribe events returns an EmptyEventKeys error on sending no event keys in subscription| ESW-93, ESW-216, ESW-86" in {
      val eventClient: EventClient = new EventClient(gatewayPostClient, gatewayWsClient)

      val exception = intercept[Exception] {
        eventClient.subscribe(Set.empty, None).runForeach(_ => ()).futureValue
      }
      exception.getCause shouldBe a[GatewayException]
    }

    "support pubsub of large events" in {
      val eventClient: EventClient = new EventClient(gatewayPostClient, gatewayWsClient)

      val eventsF = eventClient.subscribe(Set(largeEvent.eventKey), None).take(2).runWith(Sink.seq)
      Thread.sleep(500)

      eventClient.publish(largeEvent).futureValue should ===(Done)
      eventsF.futureValue.toSet shouldBe Set(invalidEvent3, largeEvent)
    }

    "subscribe observe events | ESW-581" in {
      val prefix      = Prefix("tcs.test.gateway")
      val e1          = IRDetectorEvent.observeStart(prefix)
      val e2          = IRDetectorEvent.observeStart(prefix)
      val eventClient = new EventClient(gatewayPostClient, gatewayWsClient)
      val obsEventsF  = eventClient.subscribeObserveEvents().take(2).runWith(Sink.seq)
      Thread.sleep(500)
      eventClient.publish(e1).futureValue should ===(Done)
      eventClient.publish(e2).futureValue should ===(Done)

      obsEventsF.futureValue.toSet shouldBe Set(e1, e2)
    }
  }
}

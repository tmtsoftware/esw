package esw.gateway.server.routes

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.model.{MediaTypes, StatusCodes}
import akka.http.scaladsl.unmarshalling.sse.EventStreamUnmarshalling._
import akka.stream.scaladsl.{Sink, Source}
import akka.{Done, NotUsed}
import csw.event.api.scaladsl.EventSubscription
import csw.event.api.scaladsl.SubscriptionModes.RateLimiterMode
import csw.params.core.models.{Prefix, Subsystem}
import csw.params.events._
import esw.gateway.server.{CswContextMocks, RateLimiterStub}
import esw.template.http.server.HttpTestSuite
import play.api.libs.json.Json

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class EventRoutesTest extends HttpTestSuite {

  private val actorSystem: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "test-system")

  trait Setup {
    val cswMocks = new CswContextMocks(actorSystem)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    actorSystem.terminate()
  }

  val tcsEventKeyStr1 = "tcs.event.key1"
  val tcsEventKeyStr2 = "tcs.event.key2"
  val eventKey1       = EventKey(tcsEventKeyStr1)
  val eventKey2       = EventKey(tcsEventKeyStr2)

  val event1 = ObserveEvent(Prefix("tsc"), EventName("event.key1"))
  val event2 = ObserveEvent(Prefix("tsc"), EventName("event.key2"))

  val eventSubscription: EventSubscription = new EventSubscription {
    override def unsubscribe(): Future[Done] = Future.successful(Done)

    override def ready(): Future[Done] = Future.successful(Done)
  }

  val eventSource: Source[Event, EventSubscription] =
    Source(Set(event1, event2)).mapMaterializedValue(_ => eventSubscription)

  "EventRoutes for /event" must {
    "get event for event keys | ESW-94" in new Setup {
      import cswMocks._

      val expectedEvents: Set[Event] = Set(event1, event2)
      when(eventSubscriber.get(Set(eventKey1, eventKey2))).thenReturn(Future.successful(expectedEvents))

      Get(s"/event?key=$tcsEventKeyStr1&key=$tcsEventKeyStr2") ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Set[Event]] shouldBe expectedEvents
      }
    }

    "get fail if future fails | ESW-94" in new Setup {
      import cswMocks._
      when(eventSubscriber.get(Set(eventKey1, eventKey2))).thenReturn(Future.failed(new RuntimeException("failed")))

      Get(s"/event?key=$tcsEventKeyStr1&key=$tcsEventKeyStr2") ~> route ~> check {
        status shouldBe StatusCodes.InternalServerError
      }
    }

    "post event | ESW-92" in new Setup {
      import cswMocks._
      when(eventPublisher.publish(event1)).thenReturn(Future.successful(Done))

      Post(s"/event", event1) ~> route ~> check {
        status shouldBe StatusCodes.OK
        verify(eventPublisher).publish(event1)
      }
    }

    "post event fail if future fails | ESW-92" in new Setup {
      import cswMocks._
      when(eventPublisher.publish(event1)).thenReturn(Future.failed(new RuntimeException("failed")))

      Post(s"/event", event1) ~> route ~> check {
        status shouldBe StatusCodes.InternalServerError
        verify(eventPublisher).publish(event1)
      }
    }

    "/subscribe" must {
      "subscribe to events for event keys with given frequency" in new Setup {
        import cswMocks._

        when(eventSubscriber.subscribe(Set(eventKey1, eventKey2), 100.millis, RateLimiterMode)).thenReturn(eventSource)

        Get(s"/event/subscribe?key=$eventKey1&key=$eventKey2&frequency=10") ~> route ~> check {
          status shouldBe StatusCodes.OK
          mediaType shouldBe MediaTypes.`text/event-stream`
          verify(eventSubscriber).subscribe(Set(eventKey1, eventKey2), 100.millis, RateLimiterMode)

          val actualDataF: Future[Seq[Event]] = responseAs[Source[ServerSentEvent, NotUsed]]
            .map(sse => Json.fromJson[Event](Json.parse(sse.getData())).get)
            .runWith(Sink.seq)

          Await.result(actualDataF, 5.seconds) shouldEqual Seq(event1, event2)
        }
      }

      "subscribe throws exception" in new Setup {
        import cswMocks._

        when(eventSubscriber.subscribe(Set(eventKey1, eventKey2), 100.millis, RateLimiterMode))
          .thenThrow(new RuntimeException("exception"))

        Get(s"/event/subscribe?key=$eventKey1&key=$eventKey2&frequency=10") ~> route ~> check {
          status shouldBe StatusCodes.InternalServerError
          verify(eventSubscriber).subscribe(Set(eventKey1, eventKey2), 100.millis, RateLimiterMode)
        }
      }

      "subscribe to events matching for given subsystem with specified pattern" in new Setup {
        import cswMocks._
        val subsystemName        = "tcs"
        val pattern              = "event"
        val subsystem: Subsystem = Subsystem.withName(subsystemName)

        when(eventSubscriber.pSubscribe(subsystem, pattern)).thenReturn(eventSource)

        when(eventSubscriberUtil.subscriptionModeStage(100.millis, RateLimiterMode))
          .thenReturn(new RateLimiterStub[Event](100.millis))

        Get(s"/event/subscribe/$subsystemName?frequency=10&pattern=$pattern") ~> route ~> check {
          status shouldBe StatusCodes.OK
          mediaType shouldBe MediaTypes.`text/event-stream`

          //check is psubscribe is called with specified pattern
          verify(eventSubscriber).pSubscribe(subsystem, pattern)
          verify(eventSubscriberUtil).subscriptionModeStage(100.millis, RateLimiterMode)
        }
      }

      "subscribe to events matching for given subsystem if pattern not provided" in new Setup {
        import cswMocks._
        val subsystemName        = "tcs"
        val subsystem: Subsystem = Subsystem.withName(subsystemName)

        when(eventSubscriber.pSubscribe(subsystem, "*")).thenReturn(eventSource)
        when(eventSubscriberUtil.subscriptionModeStage(100.millis, RateLimiterMode))
          .thenReturn(new RateLimiterStub[Event](100.millis))

        Get(s"/event/subscribe/$subsystemName?frequency=10") ~> route ~> check {
          status shouldBe StatusCodes.OK
          mediaType shouldBe MediaTypes.`text/event-stream`

          //check is psubscribe is called with * pattern
          verify(eventSubscriber).pSubscribe(subsystem, "*")
          verify(eventSubscriberUtil).subscriptionModeStage(100.millis, RateLimiterMode)
        }
      }

      "subscribe to events matching for given subsystem throws exception" in new Setup {
        import cswMocks._
        val subsystemName        = "tcs"
        val subsystem: Subsystem = Subsystem.withName(subsystemName)

        when(eventSubscriber.pSubscribe(subsystem, "*"))
          .thenThrow(new RuntimeException("exception"))

        Get(s"/event/subscribe/$subsystemName?frequency=10") ~> route ~> check {
          status shouldBe StatusCodes.InternalServerError
          verify(eventSubscriber).pSubscribe(subsystem, "*")
        }
      }

      "subscribe to events matching for given subsystem should rate limit to given frequency" in new Setup {
        import cswMocks._

        val subsystemName        = "tcs"
        val subsystem: Subsystem = Subsystem.withName(subsystemName)

        val totalEvents = 40
        val eventSourceStream: Source[SystemEvent, EventSubscription] = Source(1 to totalEvents)
          .map(x => SystemEvent(Prefix("tcs"), EventName(x.toString)))
          .mapMaterializedValue(_ => eventSubscription)

        when(eventSubscriber.pSubscribe(subsystem, "*")).thenReturn(eventSourceStream)
        when(eventSubscriberUtil.subscriptionModeStage(200.millis, RateLimiterMode))
          .thenReturn(new RateLimiterStub[Event](200.millis))

        Get(s"/event/subscribe/$subsystemName?frequency=5") ~> route ~> check {
          status shouldBe StatusCodes.OK
          mediaType shouldBe MediaTypes.`text/event-stream`
          verify(eventSubscriber).pSubscribe(subsystem, "*")
          verify(eventSubscriberUtil).subscriptionModeStage(200.millis, RateLimiterMode)

          val actualDataF: Future[Seq[Event]] = responseAs[Source[ServerSentEvent, NotUsed]]
            .map(sse => Json.fromJson[Event](Json.parse(sse.getData())).get)
            .runWith(Sink.seq)

          val events = Await.result(actualDataF, 5.seconds)
          events.length shouldBe totalEvents
        }
      }
    }
  }
}

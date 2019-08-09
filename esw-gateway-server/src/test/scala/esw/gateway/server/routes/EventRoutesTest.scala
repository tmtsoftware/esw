package esw.gateway.server.routes

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.model.{MediaTypes, StatusCodes}
import akka.http.scaladsl.unmarshalling.sse.EventStreamUnmarshalling._
import akka.stream.scaladsl.{Sink, Source}
import akka.{Done, NotUsed}
import csw.commons.http.ErrorResponse
import csw.event.api.scaladsl.EventSubscription
import csw.event.api.scaladsl.SubscriptionModes.RateLimiterMode
import csw.params.core.models.{Prefix, Subsystem}
import csw.params.events._
import esw.gateway.server.{CswContextMocks, RateLimiterStub}
import esw.http.core.HttpTestSuite
import io.bullet.borer.Json
import esw.http.core.codec.ErrorCodecs._

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

  private val tcsEventKeyStr1 = "tcs.event.key1"
  private val tcsEventKeyStr2 = "tcs.event.key2"
  private val eventKey1       = EventKey(tcsEventKeyStr1)
  private val eventKey2       = EventKey(tcsEventKeyStr2)

  private val event1: Event = ObserveEvent(Prefix("tcs"), EventName("event.key1"))
  private val event2: Event = ObserveEvent(Prefix("tcs"), EventName("event.key2"))

  private val eventSubscription: EventSubscription = new EventSubscription {
    override def unsubscribe(): Future[Done] = Future.successful(Done)

    override def ready(): Future[Done] = Future.successful(Done)
  }

  private val eventSource: Source[Event, EventSubscription] =
    Source(Set(event1, event2)).mapMaterializedValue(_ => eventSubscription)

  "GET /event" must {
    "return events for given event keys | ESW-94" in new Setup {
      import cswMocks._

      val expectedEvents: Set[Event] = Set(event1, event2)
      when(eventSubscriber.get(Set(eventKey1, eventKey2))).thenReturn(Future.successful(expectedEvents))

      Get(s"/event?key=$tcsEventKeyStr1&key=$tcsEventKeyStr2") ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Set[Event]] shouldBe expectedEvents
      }
    }

    "return BadRequest if event keys are not provided | ESW-94" in new Setup {
      import cswMocks._

      Get(s"/event") ~> route ~> check {
        responseAs[ErrorResponse].error.code shouldBe StatusCodes.BadRequest.intValue
        responseAs[ErrorResponse].error.message shouldBe "Request is missing query parameter key"
      }
    }

    "return InternalServerError if internal api call fails | ESW-94" in new Setup {
      import cswMocks._
      when(eventSubscriber.get(Set(eventKey1, eventKey2))).thenReturn(Future.failed(new RuntimeException("failed")))

      Get(s"/event?key=$tcsEventKeyStr1&key=$tcsEventKeyStr2") ~> route ~> check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "POST /event" must {
    "publish the event using eventPublisher" in new Setup {
      import cswMocks._
      when(eventPublisher.publish(event1)).thenReturn(Future.successful(Done))

      Post(s"/event", event1) ~> route ~> check {
        status shouldBe StatusCodes.OK
        verify(eventPublisher).publish(event1)
      }
    }

    "return BadRequest if event json is invalid" in new Setup {
      import cswMocks._

      Post(s"/event", "bad event") ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "return InternalServerError if event publishing fails due to RuntimeException exception | ESW-92" in new Setup {
      import cswMocks._
      when(eventPublisher.publish(event1)).thenReturn(Future.failed(new RuntimeException("failed")))

      Post(s"/event", event1) ~> route ~> check {
        status shouldBe StatusCodes.InternalServerError
        verify(eventPublisher).publish(event1)
      }
    }
  }

  "GET /event/subscribe" must {

    "return a stream of events for given event keys | ESW-93" in new Setup {
      import cswMocks._

      when(eventSubscriber.subscribe(Set(eventKey1, eventKey2))).thenReturn(eventSource)

      Get(s"/event/subscribe?key=$eventKey1&key=$eventKey2") ~> route ~> check {
        status shouldBe StatusCodes.OK
        mediaType shouldBe MediaTypes.`text/event-stream`
        verify(eventSubscriber).subscribe(Set(eventKey1, eventKey2))

        val actualDataF: Future[Seq[Event]] = responseAs[Source[ServerSentEvent, NotUsed]]
          .map(sse => Json.decode(sse.getData().getBytes("utf8")).to[Event].value)
          .runWith(Sink.seq)

        Await.result(actualDataF, 5.seconds) shouldEqual Seq(event1, event2)
      }
    }

    "return a stream of events for given keys with given frequency | ESW-93" in new Setup {
      import cswMocks._

      when(eventSubscriber.subscribe(Set(eventKey1, eventKey2), 100.millis, RateLimiterMode)).thenReturn(eventSource)

      Get(s"/event/subscribe?key=$eventKey1&key=$eventKey2&max-frequency=10") ~> route ~> check {
        status shouldBe StatusCodes.OK
        mediaType shouldBe MediaTypes.`text/event-stream`
        verify(eventSubscriber).subscribe(Set(eventKey1, eventKey2), 100.millis, RateLimiterMode)

        val actualDataF: Future[Seq[Event]] = responseAs[Source[ServerSentEvent, NotUsed]]
          .map(sse => Json.decode(sse.getData().getBytes("utf8")).to[Event].value)
          .runWith(Sink.seq)

        Await.result(actualDataF, 5.seconds) shouldEqual Seq(event1, event2)
      }
    }

    "return BadRequest if no keys provided | ESW-93 " in new Setup {
      import cswMocks._

      Get(s"/event/subscribe?max-frequency=10") ~> route ~> check {
        responseAs[ErrorResponse].error.code shouldBe StatusCodes.BadRequest.intValue
        responseAs[ErrorResponse].error.message shouldBe "Request is missing query parameter key"
      }
    }

    "return BadRequest if provided max frequency <= 0 | ESW-93" in new Setup {
      import cswMocks._

      Get(s"/event/subscribe?key=tcs.gateway&max-frequency=-20") ~> route ~> check {
        responseAs[ErrorResponse].error.code shouldBe StatusCodes.BadRequest.intValue
        responseAs[ErrorResponse].error.message shouldBe "Max frequency should be greater than zero"
      }
    }

    "return InternalServerError if internal subscribe API fails | ESW-93" in new Setup {
      import cswMocks._

      when(eventSubscriber.subscribe(Set(eventKey1, eventKey2), 100.millis, RateLimiterMode))
        .thenThrow(new RuntimeException("exception"))

      Get(s"/event/subscribe?key=$eventKey1&key=$eventKey2&max-frequency=10") ~> route ~> check {
        status shouldBe StatusCodes.InternalServerError
        verify(eventSubscriber).subscribe(Set(eventKey1, eventKey2), 100.millis, RateLimiterMode)
      }
    }
  }

  "GET /event/subscribe/{subsystemName}" must {

    "return a stream of events matching for given subsystem | ESW-93" in new Setup {
      import cswMocks._

      val subsystemName        = "tcs"
      val subsystem: Subsystem = Subsystem.withName(subsystemName)

      val totalEvents = 40
      val eventSourceStream: Source[SystemEvent, EventSubscription] = Source(1 to totalEvents)
        .map(x => SystemEvent(Prefix("tcs"), EventName(x.toString)))
        .mapMaterializedValue(_ => eventSubscription)

      when(eventSubscriber.pSubscribe(subsystem, "*")).thenReturn(eventSourceStream)

      Get(s"/event/subscribe/$subsystemName") ~> route ~> check {
        status shouldBe StatusCodes.OK
        mediaType shouldBe MediaTypes.`text/event-stream`
        verify(eventSubscriber).pSubscribe(subsystem, "*")
        verifyZeroInteractions(eventSubscriberUtil)

        val actualDataF: Future[Seq[Event]] = responseAs[Source[ServerSentEvent, NotUsed]]
          .map(sse => Json.decode(sse.getData().getBytes("utf8")).to[Event].value)
          .runWith(Sink.seq)

        val events = Await.result(actualDataF, 5.seconds)
        events.length shouldBe totalEvents
      }
    }

    "return a stream of events matching given subsystem with given pattern | ESW-93" in new Setup {
      import cswMocks._
      val subsystemName        = "tcs"
      val pattern              = "event"
      val subsystem: Subsystem = Subsystem.withName(subsystemName)

      when(eventSubscriber.pSubscribe(subsystem, pattern)).thenReturn(eventSource)

      when(eventSubscriberUtil.subscriptionModeStage(100.millis, RateLimiterMode))
        .thenReturn(new RateLimiterStub[Event](100.millis))

      Get(s"/event/subscribe/$subsystemName?max-frequency=10&pattern=$pattern") ~> route ~> check {
        status shouldBe StatusCodes.OK
        mediaType shouldBe MediaTypes.`text/event-stream`

        //check is psubscribe is called with specified pattern
        verify(eventSubscriber).pSubscribe(subsystem, pattern)
        verify(eventSubscriberUtil).subscriptionModeStage(100.millis, RateLimiterMode)
      }
    }

    "return a stream of events matching given subsystem with given max-frequency | ESW-93" in new Setup {
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

      Get(s"/event/subscribe/$subsystemName?max-frequency=5") ~> route ~> check {
        status shouldBe StatusCodes.OK
        mediaType shouldBe MediaTypes.`text/event-stream`
        verify(eventSubscriber).pSubscribe(subsystem, "*")
        verify(eventSubscriberUtil).subscriptionModeStage(200.millis, RateLimiterMode)

        val actualDataF: Future[Seq[Event]] = responseAs[Source[ServerSentEvent, NotUsed]]
          .map(sse => Json.decode(sse.getData().getBytes("utf8")).to[Event].value)
          .runWith(Sink.seq)

        val events = Await.result(actualDataF, 5.seconds)
        events.length shouldBe totalEvents
      }
    }

    "subscribe to events matching for given subsystem without max-frequency | ESW-93" in new Setup {
      import cswMocks._

      val subsystemName        = "tcs"
      val subsystem: Subsystem = Subsystem.withName(subsystemName)

      val totalEvents = 40
      val eventSourceStream: Source[SystemEvent, EventSubscription] = Source(1 to totalEvents)
        .map(x => SystemEvent(Prefix("tcs"), EventName(x.toString)))
        .mapMaterializedValue(_ => eventSubscription)

      when(eventSubscriber.pSubscribe(subsystem, "*")).thenReturn(eventSourceStream)

      Get(s"/event/subscribe/$subsystemName") ~> route ~> check {
        status shouldBe StatusCodes.OK
        mediaType shouldBe MediaTypes.`text/event-stream`
        verify(eventSubscriber).pSubscribe(subsystem, "*")
        verifyZeroInteractions(eventSubscriberUtil)

        val actualDataF: Future[Seq[Event]] = responseAs[Source[ServerSentEvent, NotUsed]]
          .map(sse => Json.decode(sse.getData().getBytes("utf8")).to[Event].value)
          .runWith(Sink.seq)

        val events = Await.result(actualDataF, 5.seconds)
        events.length shouldBe totalEvents
      }
    }

    "return InternalServerError if psubscribe API fails | ESW-93" in new Setup {
      import cswMocks._
      val subsystemName        = "tcs"
      val subsystem: Subsystem = Subsystem.withName(subsystemName)

      when(eventSubscriber.pSubscribe(subsystem, "*"))
        .thenThrow(new RuntimeException("exception"))

      Get(s"/event/subscribe/$subsystemName?max-frequency=10") ~> route ~> check {
        status shouldBe StatusCodes.InternalServerError
        verify(eventSubscriber).pSubscribe(subsystem, "*")
      }
    }
  }
}

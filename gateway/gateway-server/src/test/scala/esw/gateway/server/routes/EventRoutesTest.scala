package esw.gateway.server.routes

import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.model.{MediaTypes, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.sse.EventStreamUnmarshalling._
import akka.stream.scaladsl.{Sink, Source}
import akka.{Done, NotUsed}
import csw.event.api.scaladsl.EventSubscription
import csw.event.api.scaladsl.SubscriptionModes.RateLimiterMode
import csw.params.core.formats.JsonSupport
import csw.params.core.models.{Prefix, Subsystem}
import csw.params.events.{Event, EventKey, EventName, ObserveEvent}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import esw.gateway.server.CswContextMocks
import org.mockito.Mockito.{verify, when}
import org.mockito.{ArgumentMatchersSugar, Mockito}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpec}
import play.api.libs.json.Json

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class EventRoutesTest
    extends WordSpec
    with CswContextMocks
    with Matchers
    with ArgumentMatchersSugar
    with ScalatestRouteTest
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with JsonSupport
    with PlayJsonSupport {

  val route: Route = new EventRoutes(cswCtx).route

  val tcsEventKeyStr1 = "tcs.event.key1"
  val tcsEventKeyStr2 = "tcs.event.key2"
  val eventKey1       = EventKey(tcsEventKeyStr1)
  val eventKey2       = EventKey(tcsEventKeyStr2)

  val event1 = ObserveEvent(Prefix("tsc"), EventName("event.key1"))
  val event2 = ObserveEvent(Prefix("tsc"), EventName("event.key2"))

  val eventSubscription: EventSubscription = new EventSubscription {
    override def unsubscribe(): Future[Done] = Future.successful(Done)
    override def ready(): Future[Done]       = Future.successful(Done)
  }

  val eventSource: Source[Event, EventSubscription] =
    Source(Set(event1, event2)).mapMaterializedValue(_ => eventSubscription)

  override protected def afterEach(): Unit = {
    Mockito.clearInvocations(eventPublisher, eventSubscriber)
  }

  override protected def afterAll(): Unit = cswCtx.actorSystem.terminate()

  "EventRoutes for /event" must {
    "get event for event keys | ESW-94" in {

      val expectedEvents: Set[Event] = Set(event1, event2)
      when(eventSubscriber.get(Set(eventKey1, eventKey2))).thenReturn(Future.successful(expectedEvents))

      Get(s"/event?keys=$tcsEventKeyStr1&keys=$tcsEventKeyStr2") ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Set[Event]] shouldBe expectedEvents
      }
    }

    "get fail if future fails | ESW-94" in {
      when(eventSubscriber.get(Set(eventKey1, eventKey2))).thenReturn(Future.failed(new RuntimeException("failed")))

      Get(s"/event?keys=$tcsEventKeyStr1&keys=$tcsEventKeyStr2") ~> route ~> check {
        status shouldBe StatusCodes.InternalServerError
      }
    }

    "post event | ESW-92" in {
      when(eventPublisher.publish(event1)).thenReturn(Future.successful(Done))

      Post(s"/event", event1) ~> route ~> check {
        status shouldBe StatusCodes.OK
        verify(eventPublisher).publish(event1)
      }
    }

    "post event fail if future fails | ESW-92" in {
      when(eventPublisher.publish(event1)).thenReturn(Future.failed(new RuntimeException("failed")))

      Post(s"/event", event1) ~> route ~> check {
        status shouldBe StatusCodes.InternalServerError
        verify(eventPublisher).publish(event1)
      }
    }

    "/subscribe" must {
      "subscribe to events for event keys with given frequency" in {

        when(eventSubscriber.subscribe(Set(eventKey1, eventKey2), 100.millis, RateLimiterMode)).thenReturn(eventSource)

        Get(s"/event/subscribe?keys=$eventKey1&keys=$eventKey2&frequency=10") ~> route ~> check {
          status shouldBe StatusCodes.OK
          mediaType shouldBe MediaTypes.`text/event-stream`
          verify(eventSubscriber).subscribe(Set(eventKey1, eventKey2), 100.millis, RateLimiterMode)

          val actualDataF: Future[Seq[Event]] = responseAs[Source[ServerSentEvent, NotUsed]]
            .map(sse => Json.fromJson[Event](Json.parse(sse.getData())).get)
            .runWith(Sink.seq)

          Await.result(actualDataF, 5.seconds) shouldEqual Seq(event1, event2)
        }
      }

      "subscribe throws exception" in {
        when(eventSubscriber.subscribe(Set(eventKey1, eventKey2), 100.millis, RateLimiterMode))
          .thenThrow(new RuntimeException("exception"))

        Get(s"/event/subscribe?keys=$eventKey1&keys=$eventKey2&frequency=10") ~> route ~> check {
          status shouldBe StatusCodes.InternalServerError
          verify(eventSubscriber).subscribe(Set(eventKey1, eventKey2), 100.millis, RateLimiterMode)
        }
      }

      "subscribe to events matching for given subsystem with specified pattern" in {
        val subsystemName = "tcs"
        val pattern       = "event"
        val subsystem     = Subsystem.withName(subsystemName)

        when(eventSubscriber.pSubscribe(subsystem, pattern)).thenReturn(eventSource)

        Get(s"/event/subscribe/$subsystemName?frequency=10&pattern=$pattern") ~> route ~> check {
          status shouldBe StatusCodes.OK
          mediaType shouldBe MediaTypes.`text/event-stream`
          verify(eventSubscriber).pSubscribe(subsystem, pattern)

          val actualDataF: Future[Seq[Event]] = responseAs[Source[ServerSentEvent, NotUsed]]
            .map(sse => Json.fromJson[Event](Json.parse(sse.getData())).get)
            .runWith(Sink.seq)

          Await.result(actualDataF, 5.seconds) shouldEqual Seq(event1, event2)
        }
      }

      "subscribe to events matching for given subsystem if pattern not provided" in {
        val subsystemName = "tcs"
        val subsystem     = Subsystem.withName(subsystemName)

        when(eventSubscriber.pSubscribe(subsystem, "*")).thenReturn(eventSource)

        Get(s"/event/subscribe/$subsystemName?frequency=10") ~> route ~> check {
          status shouldBe StatusCodes.OK
          mediaType shouldBe MediaTypes.`text/event-stream`
          verify(eventSubscriber).pSubscribe(subsystem, "*")

          val actualDataF: Future[Seq[Event]] = responseAs[Source[ServerSentEvent, NotUsed]]
            .map(sse => Json.fromJson[Event](Json.parse(sse.getData())).get)
            .runWith(Sink.seq)

          Await.result(actualDataF, 5.seconds) shouldEqual Seq(event1, event2)
        }
      }

      "subscribe to events matching for given subsystem throws exception" in {
        val subsystemName = "tcs"
        val subsystem     = Subsystem.withName(subsystemName)

        when(eventSubscriber.pSubscribe(subsystem, "*"))
          .thenThrow(new RuntimeException("exception"))

        Get(s"/event/subscribe/$subsystemName?frequency=10") ~> route ~> check {
          status shouldBe StatusCodes.InternalServerError
          verify(eventSubscriber).pSubscribe(subsystem, "*")
        }
      }
    }
  }
}

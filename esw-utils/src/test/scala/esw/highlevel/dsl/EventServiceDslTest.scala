package esw.highlevel.dsl

import csw.event.api.exceptions.PublishFailure
import csw.event.api.scaladsl.{EventPublisher, EventService, EventSubscriber}
import csw.params.core.models.Prefix
import csw.params.events.{Event, EventKey, EventName, SystemEvent}
import csw.time.core.models.{TMTTime, UTCTime}
import esw.ocs.api.BaseTestSuite
import org.mockito.ArgumentMatchers.{any, eq => argsEq}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

class EventServiceDslTest extends BaseTestSuite {
  private val eventService    = mock[EventService]
  private val eventPublisher  = mock[EventPublisher]
  private val eventSubscriber = mock[EventSubscriber]
  private val event           = SystemEvent(Prefix("TCS.test"), EventName("event-1"))
  private val eventServiceDsl = new EventServiceDsl(eventService)

  when(eventService.defaultPublisher).thenReturn(eventPublisher)
  when(eventService.defaultSubscriber).thenReturn(eventSubscriber)

  "publish" must {
    "delegate to publishing single event | ESW-120" in {
      eventServiceDsl.publish(event)
      verify(eventService.defaultPublisher).publish(event)
    }

    "delegate to publishing event with generator | ESW-120" in {
      eventServiceDsl.publish(5.seconds)(Some(event))
      verify(eventService.defaultPublisher).publishAsync(
        any[Future[Option[Event]]],
        any[TMTTime],
        argsEq(5.seconds),
        any[PublishFailure => Unit]()
      )
    }

    "delegate to publishing event with generator, start time and onError | ESW-120" in {
      val time    = UTCTime.now()
      val onError = (publishFailure: PublishFailure) => println(publishFailure)
      eventServiceDsl.publish(5.seconds, time)(Some(event), onError)
      verify(eventService.defaultPublisher).publishAsync(
        any[Future[Option[Event]]],
        argsEq(time),
        argsEq(5.seconds),
        argsEq(onError)
      )
    }
  }

  "subscribe" must {
    "delegate to subscribing events | ESW-120" in {
      eventServiceDsl.subscribe("TCS.test.event-1")(println)
      verify(eventService.defaultSubscriber).subscribeAsync(any[Set[EventKey]], any[Event => Future[_]]())
    }
  }

  "get" must {
    "delegate to getting events | ESW-120" in {
      eventServiceDsl.get("TCS.test.event-1")
      verify(eventService.defaultSubscriber).get(any[Set[EventKey]])
    }
  }
}

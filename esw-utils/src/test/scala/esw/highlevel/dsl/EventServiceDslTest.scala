package esw.highlevel.dsl

import csw.event.api.scaladsl.{EventPublisher, EventService, EventSubscriber}
import csw.params.core.models.Prefix
import csw.params.events.{Event, EventKey, EventName, SystemEvent}
import esw.ocs.api.BaseTestSuite
import org.mockito.ArgumentMatchers.{any, eq => argsEq}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

class EventServiceDslImpl(val eventService: EventService) extends EventServiceDsl

class EventServiceDslTest extends BaseTestSuite {
  private val eventService    = mock[EventService]
  private val eventPublisher  = mock[EventPublisher]
  private val eventSubscriber = mock[EventSubscriber]
  private val event           = SystemEvent(Prefix("TCS.test"), EventName("event-1"))

  private val eventServiceDsl = new EventServiceDslImpl(eventService)

  when(eventService.defaultPublisher).thenReturn(eventPublisher)
  when(eventService.defaultSubscriber).thenReturn(eventSubscriber)

  "publish" must {
    "delegate to publishing single event | ESW-120" in {
      eventServiceDsl.publishEvent(event)
      verify(eventService.defaultPublisher).publish(event)
    }

    "delegate to publishing event with generator | ESW-120" in {
      eventServiceDsl.publishEvent(5.seconds)(Some(event))
      verify(eventService.defaultPublisher).publishAsync(
        any[Future[Option[Event]]],
        argsEq(5.seconds)
      )
    }
  }

  "subscribe" must {
    "delegate to subscribing events | ESW-120" in {
      eventServiceDsl.onEvent("TCS.test.event-1")(println)
      verify(eventService.defaultSubscriber).subscribeAsync(any[Set[EventKey]], any[Event => Future[_]]())
    }
  }

  "get" must {
    "delegate to getting events | ESW-120" in {
      eventServiceDsl.getEvent("TCS.test.event-1")
      verify(eventService.defaultSubscriber).get(any[Set[EventKey]])
    }
  }
}

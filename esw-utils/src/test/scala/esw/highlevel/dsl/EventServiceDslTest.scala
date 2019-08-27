package esw.highlevel.dsl

import csw.event.api.scaladsl.{EventPublisher, EventService, EventSubscriber}
import csw.params.core.models.Prefix
import csw.params.events.{Event, EventKey, EventName, SystemEvent}
import esw.ocs.api.BaseTestSuite
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

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

package esw.highlevel.dsl

import csw.event.api.scaladsl.{EventPublisher, EventService}
import csw.params.core.models.Prefix
import csw.params.events.{EventName, SystemEvent}
import esw.ocs.api.BaseTestSuite
import org.mockito.Mockito.{verify, when}

class EventServiceDslTest extends BaseTestSuite {
  private val eventService    = mock[EventService]
  private val eventPublisher  = mock[EventPublisher]
  private val event           = SystemEvent(Prefix("TCS.test"), EventName("event-1"))
  private val eventServiceDsl = new EventServiceDsl(eventService)

  when(eventService.defaultPublisher).thenReturn(eventPublisher)

  "publish" must {
    "delegate to publishing single event | ESW-120" in {
      eventServiceDsl.publish(event)
      verify(eventService.defaultPublisher).publish(event)
    }
  }
}

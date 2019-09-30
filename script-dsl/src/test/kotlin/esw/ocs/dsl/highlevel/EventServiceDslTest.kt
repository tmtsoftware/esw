package esw.ocs.dsl.highlevel

import akka.Done.done
import akka.actor.Cancellable
import csw.event.api.javadsl.IEventPublisher
import csw.event.api.javadsl.IEventService
import csw.event.api.javadsl.IEventSubscriber
import csw.event.api.javadsl.IEventSubscription
import csw.params.core.models.Prefix
import csw.params.events.*
import io.kotlintest.shouldBe
import io.kotlintest.specs.WordSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Job
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.CoroutineContext
import kotlin.time.milliseconds

class EventServiceDslTest : WordSpec({

    class Mocks {
        val key = "TCS.test.eventkey1"
        val duration = 10.milliseconds

        val event: Event = mockk<ObserveEvent>()
        val eventKeys = HashSet<EventKey>()
        val eventSet = HashSet<Event>(1)
        val eventSubscription = mockk<IEventSubscription>()

        val cancellable = mockk<Cancellable>()
        val eventCallback = mockk<(Event) -> CompletableFuture<*>>()

        val eventPublisher: IEventPublisher = mockk()
        val eventSubscriber: IEventSubscriber = mockk()
        val _eventService: IEventService = mockk()

        init {
            every { _eventService.defaultPublisher() }.answers { eventPublisher }
            every { _eventService.defaultSubscriber() }.answers { eventSubscriber }
            eventKeys.add(EventKey.apply(key))
            eventSet.add(event)
        }

        val eventServiceDsl = object : EventServiceDsl {
            override val coroutineContext: CoroutineContext = Job()
            override val eventService: IEventService = _eventService
        }
    }

    "EventServiceDsl" should {

        "systemEvent should return a SystemEvent created with given parameters"{
            with(Mocks()) {
                val eventName = "systemEvent1"
                val eventPrefix = "TCS.filter.wheel"
                val systemEvent = eventServiceDsl.systemEvent(eventPrefix, eventName)

                // Verify that  event with provided prefix and eventName is created.
                systemEvent shouldBe SystemEvent(
                    systemEvent.eventId(),
                    Prefix(eventPrefix),
                    EventName(eventName),
                    systemEvent.eventTime(),
                    systemEvent.paramSet()
                )
            }
        }

        "observeEvent should return a ObserveEvent created with given parameters"{
            with(Mocks()) {
                val eventName = "observeEvent1"
                val eventPrefix = "TCS.filter.wheel"
                val observeEvent = eventServiceDsl.observeEvent(eventPrefix, eventName)

                // Verify that event with provided prefix and eventName is created.
                observeEvent shouldBe ObserveEvent(
                    observeEvent.eventId(),
                    Prefix(eventPrefix),
                    EventName(eventName),
                    observeEvent.eventTime(),
                    observeEvent.paramSet()
                )
            }
        }

        "publish should delegate to publisher.publish" {
            with(Mocks()) {
                every { (eventPublisher.publish(event)) }
                    .returns(CompletableFuture.completedFuture(done()))

                eventServiceDsl.publishEvent(event) shouldBe done()

                verify { _eventService.defaultPublisher() }
                verify { eventPublisher.publish(event) }
            }
        }

        "publishEvent should delegate to publisher.publishAsync" {
            with(Mocks()) {
                every { eventPublisher.publishAsync(any(), any()) }.answers { cancellable }

                eventServiceDsl.publishEvent(duration) { event } shouldBe cancellable

                verify { _eventService.defaultPublisher() }
                verify { eventPublisher.publishAsync(any(), any()) }
            }
        }

        "onEvent should delegate to subscriber.subscribeAsync" {
            with(Mocks()) {
                every { eventSubscriber.subscribeAsync(eventKeys, any()) }.answers { eventSubscription }

                eventServiceDsl.onEvent(key) { eventCallback } shouldBe eventSubscription

                verify { _eventService.defaultSubscriber() }
                verify { eventSubscriber.subscribeAsync(eventKeys, any()) }
            }

        }

        "getEvent should delegate to subscriber.get" {
            with(Mocks()) {
                every { eventSubscriber.get(eventKeys) }.answers { CompletableFuture.completedFuture(eventSet) }

                eventServiceDsl.getEvent(key) shouldBe eventSet

                verify { _eventService.defaultSubscriber() }
                verify { eventSubscriber.get(eventKeys) }
            }
        }
    }
})
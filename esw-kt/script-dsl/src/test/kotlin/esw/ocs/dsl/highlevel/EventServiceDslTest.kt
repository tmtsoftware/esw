package esw.ocs.dsl.highlevel

import akka.Done.done
import akka.actor.Cancellable
import csw.event.api.javadsl.IEventPublisher
import csw.event.api.javadsl.IEventSubscriber
import csw.event.api.javadsl.IEventSubscription
import csw.params.core.models.Prefix
import csw.params.events.*
import io.kotlintest.shouldBe
import io.kotlintest.specs.WordSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.milliseconds
import kotlinx.coroutines.CoroutineScope

class EventServiceDslTest : WordSpec(), EventServiceDsl {

    private val key = "TCS.test.eventkey1"
    private val duration = 10.milliseconds

    private val event: Event = mockk<ObserveEvent>()
    private val eventKeys = HashSet<EventKey>()
    private val eventSet = HashSet<Event>(1)

    private val eventSubscription = mockk<IEventSubscription>()
    private val cancellable = mockk<Cancellable>()
    private val eventCallback = mockk<(Event) -> CompletableFuture<*>>()
    private val eventPublisher: IEventPublisher = mockk()
    private val eventSubscriber: IEventSubscriber = mockk()

    override val coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)
    override val defaultPublisher: IEventPublisher = eventPublisher
    override val defaultSubscriber: IEventSubscriber = eventSubscriber

    init {
        eventKeys.add(EventKey.apply(key))
        eventSet.add(event)
        "EventServiceDsl" should {

            "systemEvent should return a SystemEvent created with given parameters | ESW-120" {
                val eventName = "systemEvent1"
                val eventPrefix = "TCS.filter.wheel"
                val systemEvent = systemEvent(eventPrefix, eventName)

                // Verify that  event with provided prefix and eventName is created.
                systemEvent shouldBe SystemEvent(
                    systemEvent.eventId(),
                    Prefix(eventPrefix),
                    EventName(eventName),
                    systemEvent.eventTime(),
                    systemEvent.paramSet()
                )
            }

            "observeEvent should return a ObserveEvent created with given parameters | ESW-120" {
                val eventName = "observeEvent1"
                val eventPrefix = "TCS.filter.wheel"
                val observeEvent = observeEvent(eventPrefix, eventName)

                // Verify that event with provided prefix and eventName is created.
                observeEvent shouldBe ObserveEvent(
                    observeEvent.eventId(),
                    Prefix(eventPrefix),
                    EventName(eventName),
                    observeEvent.eventTime(),
                    observeEvent.paramSet()
                )
            }

            "publish should delegate to publisher.publish | ESW-120" {
                every { (eventPublisher.publish(event)) }.returns(CompletableFuture.completedFuture(done()))
                publishEvent(event) shouldBe done()
                verify { eventPublisher.publish(event) }
            }

            "publishEvent should delegate to publisher.publishAsync | ESW-120" {
                every { eventPublisher.publishAsync(any(), any()) }.answers { cancellable }
                publishEvent(duration) { event } shouldBe cancellable
                verify { eventPublisher.publishAsync(any(), any()) }
            }

            "onEvent should delegate to subscriber.subscribeAsync | ESW-120" {
                every { eventSubscriber.subscribeAsync(eventKeys, any()) }.answers { eventSubscription }
                onEvent(key) { eventCallback } shouldBe eventSubscription
                verify { eventSubscriber.subscribeAsync(eventKeys, any()) }
            }

            "getEvent should delegate to subscriber.get | ESW-120" {
                every { eventSubscriber.get(eventKeys) }.answers { CompletableFuture.completedFuture(eventSet) }
                getEvent(key) shouldBe eventSet
                verify { eventSubscriber.get(eventKeys) }
            }
        }
    }
}

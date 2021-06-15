package esw.ocs.dsl.highlevel

import akka.Done.done
import akka.actor.Cancellable
import csw.event.api.javadsl.IEventPublisher
import csw.event.api.javadsl.IEventSubscriber
import csw.event.api.javadsl.IEventSubscription
import csw.params.events.*
import esw.ocs.dsl.highlevel.models.EventSubscription
import esw.ocs.dsl.highlevel.models.Prefix
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

class EventServiceDslTest : EventServiceDsl {
    override val coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)
    override val eventPublisher: IEventPublisher = mockk()
    override val eventSubscriber: IEventSubscriber = mockk()

    @Test
    fun `systemEvent should return a SystemEvent created with given parameters | ESW-120`() {
        val eventName = "systemEvent1"
        val eventPrefix = "TCS.filter.wheel"
        val actualEvent = SystemEvent(eventPrefix, eventName)

        // Verify that event with provided prefix and eventName is created.
        actualEvent shouldBe SystemEvent(
                actualEvent.eventId(),
                Prefix(eventPrefix),
                EventName(eventName),
                actualEvent.eventTime(),
                actualEvent.paramSet()
        )
    }


    @Test
    fun `publish should delegate to publisher#publish | ESW-120`() = runBlocking {
        TestSetup().run {
            every { (this@EventServiceDslTest.eventPublisher.publish(event)) }.returns(CompletableFuture.completedFuture(done()))
            publishEvent(event) shouldBe done()
            verify { this@EventServiceDslTest.eventPublisher.publish(event) }
        }
    }

    @Test
    fun `publishEvent should delegate to publisher#publishAsync | ESW-120`() {
        TestSetup().run {
            every { this@EventServiceDslTest.eventPublisher.publishAsync(any(), any()) }.answers { cancellable }
            publishEvent(duration) { event } shouldBe cancellable
            verify { this@EventServiceDslTest.eventPublisher.publishAsync(any(), any()) }
        }
    }

    @Test
    fun `onEvent should delegate to subscriber#subscribeAsync | ESW-120`() = runBlocking {
        TestSetup().run {
            every { this@EventServiceDslTest.eventSubscriber.subscribeAsync(eventKeys, any()) }.answers { eventSubscription }
            every { eventSubscription.ready() }.answers { CompletableFuture.completedFuture(done()) }

            onEvent(key) { eventCallback }
            verify { this@EventServiceDslTest.eventSubscriber.subscribeAsync(eventKeys, any()) }
        }
    }

    @Test
    fun `cancel should delegate to IEventSubscription#unsubscribe() | ESW-120`() = runBlocking {
        TestSetup().run {
            every { this@EventServiceDslTest.eventSubscriber.subscribeAsync(eventKeys, any()) }.answers { eventSubscription }
            every { eventSubscription.unsubscribe() }.answers { CompletableFuture.completedFuture(done()) }
            every { eventSubscription.ready() }.answers { CompletableFuture.completedFuture(done()) }

            val subscription: EventSubscription = onEvent(key) { eventCallback }
            verify { this@EventServiceDslTest.eventSubscriber.subscribeAsync(eventKeys, any()) }

            subscription.cancel()
            verify { eventSubscription.unsubscribe() }
        }
    }

    @Test
    fun `getEvent should delegate to subscriber#get | ESW-120`() = runBlocking {
        TestSetup().run {
            //  for single key
            every { this@EventServiceDslTest.eventSubscriber.get(eventKeys.first()) }.answers { CompletableFuture.completedFuture(event) }
            getEvent(key) shouldBe event
            verify { this@EventServiceDslTest.eventSubscriber.get(eventKeys.first()) }

            //  for multiple keys
            val key2 = "TCS.test.eventkey2"
            val event2 = mockk<Event>()
            eventKeys.add(EventKey(key2))
            eventSet.add(event2)

            every { this@EventServiceDslTest.eventSubscriber.get(eventKeys) }.answers { CompletableFuture.completedFuture(eventSet) }
            getEvent(key, key2) shouldBe eventSet
            verify { this@EventServiceDslTest.eventSubscriber.get(eventKeys) }
        }
    }

    inner class TestSetup {
        val key = "TCS.test.eventkey1"
        val duration = Duration.milliseconds(10)

        val event: Event = mockk<ObserveEvent>()
        val eventKeys = HashSet<EventKey>()
        val eventSet = HashSet<Event>(1)

        val eventSubscription = mockk<IEventSubscription>()
        val cancellable = mockk<Cancellable>()
        val eventCallback = mockk<(Event) -> CompletableFuture<*>>()

        init {
            eventKeys.add(EventKey(key))
            eventSet.add(event)
        }
    }
}

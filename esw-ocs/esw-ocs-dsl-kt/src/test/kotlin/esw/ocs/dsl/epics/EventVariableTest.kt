package esw.ocs.dsl.epics

import akka.Done
import csw.event.api.javadsl.IEventPublisher
import csw.event.api.javadsl.IEventSubscriber
import csw.event.api.javadsl.IEventSubscription
import csw.event.api.scaladsl.SubscriptionModes
import csw.params.events.EventKey
import csw.params.events.EventName
import csw.params.events.SystemEvent
import csw.prefix.javadsl.JSubsystem
import csw.prefix.models.Prefix
import esw.ocs.dsl.highlevel.EventServiceDsl
import esw.ocs.dsl.highlevel.models.EventSubscription
import esw.ocs.dsl.params.booleanKey
import esw.ocs.dsl.params.intKey
import io.kotlintest.shouldBe
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import kotlin.time.milliseconds
import kotlin.time.toJavaDuration

class EventVariableTest {
    @Test
    fun `set should update local value and publish new event | ESW-132, ESW-142`() = runBlocking {
        val prefix = Prefix(JSubsystem.TCS(), "test")
        val eventName = EventName("testEvent")
        val systemEvent = SystemEvent(prefix, eventName)
        val booleanKey = booleanKey("testKey")
        val done = Done.done()

        val eventServiceDsl = mockk<EventServiceDsl>()
        coEvery { eventServiceDsl.publishEvent(any()) }.returns(done)

        val eventVariable: EventVariable<Boolean> = EventVariable(systemEvent, booleanKey, eventService = eventServiceDsl)
        eventVariable.set(true)

        coVerify { eventServiceDsl.publishEvent(any()) }
        eventVariable.get() shouldBe true
    }

    @Test
    fun `get should return the specific key from parameters of latest event | ESW-132, ESW-142`() = runBlocking {
        val eventServiceDsl = mockk<EventServiceDsl>()

        val intKey = intKey("testKey")
        val intValue = 10
        val systemEvent = SystemEvent(Prefix(JSubsystem.TCS(), "test"), EventName("testEvent")).add(intKey.set(intValue))

        val eventVariable: EventVariable<Int> = EventVariable(systemEvent, intKey, eventService = eventServiceDsl)

        eventVariable.get() shouldBe intValue
    }


    // Scenario: bind(fsm1) => bind(fsm2) => cancel1() => cancel2() => bind(fsm3)
    @Test
    fun `bind should start subscription and add subscription entry in Fsm | ESW-132, ESW-142`() = runBlocking {
        val eventServiceDsl = mockk<EventServiceDsl>()
        val refreshable1 = mockk<Refreshable>()
        val refreshable2 = mockk<Refreshable>()
        val refreshable3 = mockk<Refreshable>()
        val eventSubscription = mockk<EventSubscription>()

        val intKey = intKey("testKey")
        val intValue = 10
        val systemEvent = SystemEvent(Prefix(JSubsystem.TCS(), "test"), EventName("testEvent")).add(intKey.set(intValue))
        val eventKey = systemEvent.eventKey().key()

        every { refreshable1.addFsmSubscription(any()) } just runs
        every { refreshable2.addFsmSubscription(any()) } just runs
        every { refreshable3.addFsmSubscription(any()) } just runs

        coEvery { eventServiceDsl.onEvent(eventKey, callback = any()) }.returns(eventSubscription)
        coEvery { eventSubscription.cancel() } just runs

        val eventVariable: EventVariable<Int> = EventVariable(systemEvent, intKey, eventService = eventServiceDsl)

        val fsmSubscription1 = eventVariable.bind(refreshable1)
        val fsmSubscription2 = eventVariable.bind(refreshable2)

        coVerify { refreshable1.addFsmSubscription(any()) }
        coVerify { refreshable2.addFsmSubscription(any()) }
        coVerify { eventServiceDsl.onEvent(eventKey, callback = any()) }

        fsmSubscription1.cancel()
        coVerify(exactly = 0) { eventSubscription.cancel() }

        fsmSubscription2.cancel()
        coVerify { eventSubscription.cancel() }

        eventVariable.bind(refreshable3)
        coVerify { refreshable3.addFsmSubscription(any()) }
        coVerify { eventServiceDsl.onEvent(eventKey, callback = any()) }
    }

    @Test
    fun `bind with duration should start polling the event key and refresh Fsm on changes | ESW-142, ESW-256`() = runBlocking {
        val subscriber = mockk<IEventSubscriber>()
        val publisher = mockk<IEventPublisher>()
        val eventServiceDsl = object : EventServiceDsl {
            override val coroutineScope: CoroutineScope = this@runBlocking
            override val eventPublisher: IEventPublisher = publisher
            override val eventSubscriber: IEventSubscriber = subscriber
        }

        val refreshable = mockk<Refreshable>()
        val eventSubscription = mockk<IEventSubscription>()
        val doneF = CompletableFuture.completedFuture(Done.getInstance())

        val intKey = intKey("testKey")
        val intValue = 10
        val systemEvent = SystemEvent(Prefix(JSubsystem.TCS(), "test"), EventName("testEvent")).add(intKey.set(intValue))

        val eventKey = setOf(EventKey.apply(systemEvent.eventKey().key()))
        val duration = 100.milliseconds

        every { refreshable.addFsmSubscription(any()) } just runs
        every { subscriber.subscribeAsync(any(), any(), any(), any()) }.answers { eventSubscription }

        every { eventSubscription.ready() }.returns(doneF)
        every { eventSubscription.unsubscribe() }.returns(doneF)

        val eventVariable = EventVariable(systemEvent, intKey, duration, eventServiceDsl)
        val fsmSubscription = eventVariable.bind(refreshable)

        coVerify { refreshable.addFsmSubscription(any()) }
        coVerify { subscriber.subscribeAsync(eq(eventKey), any(), eq(duration.toJavaDuration()), eq(SubscriptionModes.jRateAdapterMode())) }

        fsmSubscription.cancel()
        verify { eventSubscription.unsubscribe() }

    }
}

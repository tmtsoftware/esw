package esw.ocs.dsl.epics

import akka.Done
import csw.event.api.javadsl.IEventPublisher
import csw.event.api.javadsl.IEventSubscriber
import csw.event.api.javadsl.IEventSubscription
import csw.event.api.scaladsl.SubscriptionModes
import csw.params.events.Event
import csw.params.events.EventKey
import csw.params.events.EventName
import csw.params.events.SystemEvent
import csw.prefix.models.Prefix
import esw.ocs.dsl.highlevel.EventServiceDsl
import esw.ocs.dsl.highlevel.models.EventSubscription
import esw.ocs.dsl.highlevel.models.TCS
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
    fun `make should create EventVariable by getting event from event service`() = runBlocking {
        TestSetup().run {
            coEvery { eventServiceDsl.getEvent(eventKeyStr) }.returns(mutableSetOf(systemEvent))
            val eventVariable = EventVariable.make(eventKey, eventServiceDsl)

            coVerify { eventServiceDsl.getEvent(eventKeyStr) }
            val event = eventVariable.getEvent()
            event.eventName() shouldBe eventName
            event.source() shouldBe prefix
        }
    }

    @Test
    fun `getEvent should return the latest event | ESW-291`() = runBlocking {
        TestSetup().run {
            coEvery { eventServiceDsl.getEvent(eventKeyStr) }.returns(mutableSetOf(systemEvent))
            val eventVariable = EventVariable.make(eventKey, eventServiceDsl)

            val event = eventVariable.getEvent()
            event.eventName() shouldBe eventName
            event.source() shouldBe prefix
        }
    }

    // Scenario: bind(fsm1) => bind(fsm2) => cancel1() => cancel2() => bind(fsm3)
    @Test
    fun `bind should start subscription and add subscription entry in Fsm | ESW-132, ESW-142`() = runBlocking {
        TestSetup().run {
            val refreshable1 = mockk<Refreshable>()
            val refreshable2 = mockk<Refreshable>()
            val refreshable3 = mockk<Refreshable>()
            val eventSubscription = mockk<EventSubscription>()

            every { refreshable1.addFsmSubscription(any()) } just runs
            every { refreshable2.addFsmSubscription(any()) } just runs
            every { refreshable3.addFsmSubscription(any()) } just runs

            coEvery { eventServiceDsl.getEvent(eventKeyStr) }.returns(mutableSetOf(systemEvent))
            coEvery { eventServiceDsl.publishEvent(any()) }.returns(Done.done())
            coEvery { eventServiceDsl.onEvent(eventKeyStr, callback = any()) }.returns(eventSubscription)
            coEvery { eventSubscription.cancel() } just runs

            val paramVariable = EventVariable.make(eventKey, eventServiceDsl)

            val fsmSubscription1 = paramVariable.bind(refreshable1)
            val fsmSubscription2 = paramVariable.bind(refreshable2)

            coVerify { refreshable1.addFsmSubscription(any()) }
            coVerify { refreshable2.addFsmSubscription(any()) }
            coVerify { eventServiceDsl.onEvent(eventKeyStr, callback = any()) }

            fsmSubscription1.cancel()
            coVerify(exactly = 0) { eventSubscription.cancel() }

            fsmSubscription2.cancel()
            coVerify { eventSubscription.cancel() }

            paramVariable.bind(refreshable3)
            coVerify { refreshable3.addFsmSubscription(any()) }
            coVerify { eventServiceDsl.onEvent(eventKeyStr, callback = any()) }
        }
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

        val systemEvent: Event = SystemEvent(Prefix(TCS, "test"), EventName("testEvent"))

        val refreshable = mockk<Refreshable>()
        val eventSubscription = mockk<IEventSubscription>()
        val doneF = CompletableFuture.completedFuture(Done.getInstance())
        val eventSetF = CompletableFuture.completedFuture(mutableSetOf(systemEvent))


        val eventKeyStr = systemEvent.eventKey().key()
        val eventKey = EventKey.apply(eventKeyStr)
        val duration = 100.milliseconds

        every { refreshable.addFsmSubscription(any()) } just runs
        every { subscriber.subscribeAsync(any(), any(), any(), any()) }.answers { eventSubscription }
        every { subscriber.get(mutableSetOf(eventKey)) }.returns(eventSetF)

        every { eventSubscription.ready() }.returns(doneF)
        every { eventSubscription.unsubscribe() }.returns(doneF)
        every { publisher.publish(any()) }.returns(doneF)

        val eventVariableImpl = EventVariable.make(eventKey, eventServiceDsl, duration)
        val fsmSubscription = eventVariableImpl.bind(refreshable)

        coVerify { refreshable.addFsmSubscription(any()) }
        coVerify { subscriber.subscribeAsync(eq(setOf(eventKey)), any(), eq(duration.toJavaDuration()), eq(SubscriptionModes.jRateAdapterMode())) }

        fsmSubscription.cancel()
        verify { eventSubscription.unsubscribe() }
    }

    private inner class TestSetup {
        val eventName = EventName("testEvent")
        val prefix = Prefix(TCS, "test")

        val systemEvent = SystemEvent(prefix, eventName)
        val eventKey = EventKey(prefix, eventName)
        val eventKeyStr = eventKey.key()

        val eventServiceDsl = mockk<EventServiceDsl>()
    }

}
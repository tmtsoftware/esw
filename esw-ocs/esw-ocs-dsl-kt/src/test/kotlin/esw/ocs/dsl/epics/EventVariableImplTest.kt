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
import esw.ocs.dsl.params.first
import esw.ocs.dsl.params.intKey
import io.kotlintest.shouldBe
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import kotlin.time.milliseconds
import kotlin.time.toJavaDuration

class EventVariableImplTest {
    @Test
    fun `createEventVariable should create instance of EventVariable which will be tied to Event | ESW-291`() = TestSetup().run {
        val eventVariable: EventVariable = EventVariableImpl.createEventVariable(eventKeyStr, eventServiceDsl)

        val event = eventVariable.getEvent()
        event.eventName() shouldBe eventName
        event.source() shouldBe prefix
    }

    @Test
    fun `createParamVariable should create instance of ParamVariable with given Key and initial value | ESW-291`() = TestSetup().run {
        val intValue = 5

        val eventVariable: EventVariable = EventVariableImpl.createParamVariable(intValue, intKey, eventKeyStr, eventServiceDsl)

        val event = eventVariable.getEvent()
        event.eventName() shouldBe eventName
        event.source() shouldBe prefix
        event.paramType().get(intKey).get().first shouldBe 5
    }

    @Test
    fun `getParam should return the values of given Key from Parameters of Event | ESW-132, ESW-142`() = runBlocking {
        TestSetup().run {
            val intValue = 6

            val eventVariableImpl = EventVariableImpl.createParamVariable(intValue, intKey, eventKeyStr, eventServiceDsl)

            eventVariableImpl.getParam() shouldBe intKey.set(intValue)
        }
    }

    @Test
    fun `first should return first value of given Key from Parameter of the Event | ESW-132, ESW-142`() = runBlocking {
        TestSetup().run {
            val intValue = 10

            val eventVariableImpl = EventVariableImpl.createParamVariable(intValue, intKey, eventKeyStr, eventServiceDsl)

            eventVariableImpl.first() shouldBe intValue
        }
    }

    @Test
    fun `setParam should publish new event with the new value of the parameter | ESW-132, ESW-142`() = runBlocking {
        TestSetup().run {
            coEvery { eventServiceDsl.publishEvent(any()) }.returns(Done.done())
            val paramVariable =
                    EventVariableImpl.createParamVariable(0, intKey, eventKeyStr, eventServiceDsl)

            val newValue = 100
            paramVariable.setParam(newValue)

            val eventSlot: CapturingSlot<Event> = slot()
            coVerify { eventServiceDsl.publishEvent(capture(eventSlot)) }
            eventSlot.captured.paramType().get(intKey).get().first shouldBe newValue // assert on the event which is being published
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

            coEvery { eventServiceDsl.onEvent(eventKeyStr, callback = any()) }.returns(eventSubscription)
            coEvery { eventSubscription.cancel() } just runs

            val intValue = 10
            val paramVariable = EventVariableImpl.createParamVariable(intValue, intKey, eventKeyStr, eventServiceDsl)

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

        val refreshable = mockk<Refreshable>()
        val eventSubscription = mockk<IEventSubscription>()
        val doneF = CompletableFuture.completedFuture(Done.getInstance())

        val systemEvent = SystemEvent(Prefix(TCS, "test"), EventName("testEvent"))

        val intKey = intKey("testKey")
        val intValue = 10

        val eventKeyStr = systemEvent.eventKey().key()
        val eventKey = EventKey.apply(eventKeyStr)
        val duration = 100.milliseconds

        every { refreshable.addFsmSubscription(any()) } just runs
        every { subscriber.subscribeAsync(any(), any(), any(), any()) }.answers { eventSubscription }

        every { eventSubscription.ready() }.returns(doneF)
        every { eventSubscription.unsubscribe() }.returns(doneF)

        val eventVariableImpl =
                EventVariableImpl.createParamVariable(intValue, intKey, eventKeyStr, eventServiceDsl, duration)
        val fsmSubscription = eventVariableImpl.bind(refreshable)

        coVerify { refreshable.addFsmSubscription(any()) }
        coVerify { subscriber.subscribeAsync(eq(setOf(eventKey)), any(), eq(duration.toJavaDuration()), eq(SubscriptionModes.jRateAdapterMode())) }

        fsmSubscription.cancel()
        verify { eventSubscription.unsubscribe() }
    }

    private inner class TestSetup {
        val eventName = EventName("testEvent")
        val prefix = Prefix(TCS, "test")
        val intKey = intKey("testIntKey")
        val eventKeyStr = EventKey(prefix, eventName).key()

        val eventServiceDsl = mockk<EventServiceDsl>()
    }
}

package esw.ocs.dsl.epics

import akka.Done
import csw.params.events.Event
import csw.params.events.EventKey
import csw.params.events.EventName
import csw.params.events.SystemEvent
import csw.prefix.models.Prefix
import csw.time.scheduler.api.Cancellable
import esw.ocs.dsl.SuspendableCallback
import esw.ocs.dsl.highlevel.CswHighLevelDslApi
import esw.ocs.dsl.highlevel.models.EventSubscription
import esw.ocs.dsl.highlevel.models.TCS
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

class EventVariableTest {

    @Test
    fun `make should create EventVariable by getting event from event service`() = runBlocking {
        TestSetup().run {
            coEvery { cswHighLevelDsl.getEvent(eventKeyStr) }.returns(systemEvent)
            val eventVariable = EventVariable.make(eventKey, cswHighLevelDsl)

            coVerify { cswHighLevelDsl.getEvent(eventKeyStr) }
            val event = eventVariable.getEvent()
            event.eventName() shouldBe eventName
            event.source() shouldBe prefix
        }
    }

    @Test
    fun `getEvent should return the latest event | ESW-291`() = runBlocking {
        TestSetup().run {
            coEvery { cswHighLevelDsl.getEvent(eventKeyStr) }.returns(systemEvent)
            val eventVariable = EventVariable.make(eventKey, cswHighLevelDsl)

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

            coEvery { cswHighLevelDsl.getEvent(eventKeyStr) }.returns(systemEvent)
            coEvery { cswHighLevelDsl.publishEvent(any()) }.returns(Done.done())
            coEvery { cswHighLevelDsl.onEvent(eventKeyStr, callback = any()) }.returns(eventSubscription)
            coEvery { eventSubscription.cancel() } just runs

            val paramVariable = EventVariable.make(eventKey, cswHighLevelDsl)

            val fsmSubscription1 = paramVariable.bind(refreshable1)
            val fsmSubscription2 = paramVariable.bind(refreshable2)

            coVerify { refreshable1.addFsmSubscription(any()) }
            coVerify { refreshable2.addFsmSubscription(any()) }
            coVerify { cswHighLevelDsl.onEvent(eventKeyStr, callback = any()) }

            fsmSubscription1.cancel()
            coVerify(exactly = 0) { eventSubscription.cancel() }

            fsmSubscription2.cancel()
            coVerify { eventSubscription.cancel() }

            paramVariable.bind(refreshable3)
            coVerify { refreshable3.addFsmSubscription(any()) }
            coVerify { cswHighLevelDsl.onEvent(eventKeyStr, callback = any()) }
        }
    }

    @Test
    fun `bind with duration should start polling the event key and refresh Fsm on changes | ESW-142, ESW-256`() = runBlocking {
        val systemEvent: Event = SystemEvent(Prefix(TCS, "test"), EventName("testEvent"))
        val eventKey = systemEvent.eventKey()
        val eventKeyStr = eventKey.key()
        val duration = 100.milliseconds

        val refreshable = mockk<Refreshable>()
        val cancellable = mockk<Cancellable>()
        val cswApi = mockk<CswHighLevelDslApi>()

        every { cancellable.cancel() }.returns(true)
        coEvery { cswApi.getEvent(eventKeyStr) }.returns(systemEvent)
        coEvery { cswApi.publishEvent(any()) }.returns(Done.getInstance())
        coEvery { cswApi.schedulePeriodically(duration, any()) }.returns(cancellable)

        val fsmSubscriptionSlot = slot<FsmSubscription>()
        every { refreshable.addFsmSubscription(capture(fsmSubscriptionSlot)) } just runs

        val eventVariableImpl = EventVariable.make(eventKey, cswApi, duration)
        val fsmSubscription = eventVariableImpl.bind(refreshable)

        coVerify { cswApi.getEvent(eventKeyStr) }
        // verifies FsmSubscription is added in refreshable when binding
        coVerify { refreshable.addFsmSubscription(any()) }

        // asserts the time service is dsl is called, and the lambda passed to it calls getEvent internally
        val lambdaSlot = slot<SuspendableCallback>()
        coVerify { cswApi.schedulePeriodically(duration, capture(lambdaSlot)) }
        lambdaSlot.captured.invoke(this)
        coVerify(exactly = 2) { cswApi.getEvent(eventKeyStr) }

        // verifies the cancelling the FsmSubscription calls "cancel" of Cancellable returned by schedulePeriodically.
        fsmSubscription.cancel()
        verify { cancellable.cancel() }
    }

    private inner class TestSetup {
        val eventName = EventName("testEvent")
        val prefix = Prefix(TCS, "test")

        val systemEvent = SystemEvent(prefix, eventName)
        val eventKey = EventKey(prefix, eventName)
        val eventKeyStr = eventKey.key()

        val cswHighLevelDsl = mockk<CswHighLevelDslApi>()
    }

}

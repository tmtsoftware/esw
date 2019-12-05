package esw.ocs.dsl.epics

import akka.Done
import csw.params.core.models.Prefix
import csw.params.events.EventName
import csw.params.events.SystemEvent
import csw.params.javadsl.JSubsystem.TCS
import esw.ocs.dsl.highlevel.EventServiceDsl
import esw.ocs.dsl.highlevel.EventSubscription
import esw.ocs.dsl.params.booleanKey
import esw.ocs.dsl.params.intKey
import io.kotlintest.shouldBe
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class EventVariableTest {
    @Test
    fun `set should update local value and publish new event | ESW-132, ESW-142`() = runBlocking {
        val prefix = Prefix(TCS, "test")
        val eventName = EventName("testEvent")
        val systemEvent = SystemEvent(prefix, eventName)
        val booleanKey = booleanKey("testKey")
        val done = Done.done()

        val eventServiceDsl = mockk<EventServiceDsl>()
        coEvery { eventServiceDsl.publishEvent(any()) }.returns(done)

        val eventVariable: EventVariable<Boolean> = EventVariable(systemEvent, booleanKey, eventServiceDsl)
        eventVariable.set(true)

        coVerify { eventServiceDsl.publishEvent(any()) }
        eventVariable.get() shouldBe true
    }

    @Test
    fun `get should return the specific key from parameters of latest event | ESW-132, ESW-142`() = runBlocking {
        val eventServiceDsl = mockk<EventServiceDsl>()

        val intKey = intKey("testKey")
        val intValue = 10
        val systemEvent = SystemEvent(Prefix(TCS, "test"), EventName("testEvent")).add(intKey.set(intValue))

        val eventVariable: EventVariable<Int> = EventVariable(systemEvent, intKey, eventServiceDsl)

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
        val systemEvent = SystemEvent(Prefix(TCS, "test"), EventName("testEvent")).add(intKey.set(intValue))
        val eventKey = systemEvent.eventKey().key()

        every { refreshable1.addFsmSubscription(any()) } just runs
        every { refreshable2.addFsmSubscription(any()) } just runs
        every { refreshable3.addFsmSubscription(any()) } just runs

        coEvery { eventServiceDsl.onEvent(eventKey, callback = any()) }.returns(eventSubscription)
        coEvery { eventSubscription.cancel() } just runs

        val eventVariable: EventVariable<Int> = EventVariable(systemEvent, intKey, eventServiceDsl)

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
}

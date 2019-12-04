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

class ProcessVariableTest {
    @Test
    fun `set should update local value and publish new event | ESW-142`() = runBlocking {
        val prefix = Prefix(TCS, "test")
        val eventName = EventName("testEvent")
        val systemEvent = SystemEvent(prefix, eventName)
        val booleanKey = booleanKey("testKey")
        val done = Done.done()

        val eventServiceDsl = mockk<EventServiceDsl>()
        coEvery { eventServiceDsl.publishEvent(any()) }.returns(done)

        val processVariable: ProcessVariable<Boolean> = ProcessVariable(systemEvent, booleanKey, eventServiceDsl)
        processVariable.set(true)

        coVerify { eventServiceDsl.publishEvent(any()) }
        processVariable.get() shouldBe true
    }

    @Test
    fun `get should return the specific key from parameters of latest event | ESW-142`() = runBlocking {
        val eventServiceDsl = mockk<EventServiceDsl>()

        val intKey = intKey("testKey")
        val intValue = 10
        val systemEvent = SystemEvent(Prefix(TCS, "test"), EventName("testEvent")).add(intKey.set(intValue))

        val processVariable: ProcessVariable<Int> = ProcessVariable(systemEvent, intKey, eventServiceDsl)

        processVariable.get() shouldBe intValue
    }


    // Scenario: bind(fsm1) => bind(fsm2) => cancel1() => cancel2() => bind(fsm3)
    @Test
    fun `bind should start subscription and add subscription entry in FSM | ESW-142`() = runBlocking {
        val eventServiceDsl = mockk<EventServiceDsl>()
        val refreshable1 = mockk<Refreshable>()
        val refreshable2 = mockk<Refreshable>()
        val refreshable3 = mockk<Refreshable>()
        val eventSubscription = mockk<EventSubscription>()

        val intKey = intKey("testKey")
        val intValue = 10
        val systemEvent = SystemEvent(Prefix(TCS, "test"), EventName("testEvent")).add(intKey.set(intValue))
        val eventKey = systemEvent.eventKey().key()

        every { refreshable1.addFSMSubscription(any()) } just runs
        every { refreshable2.addFSMSubscription(any()) } just runs
        every { refreshable3.addFSMSubscription(any()) } just runs
        coEvery { eventServiceDsl.onEvent(eventKey, callback = any()) }.returns(eventSubscription)
        coEvery { eventSubscription.cancel() } just runs

        val processVariable: ProcessVariable<Int> = ProcessVariable(systemEvent, intKey, eventServiceDsl)

        val fsmSubscription1 = processVariable.bind(refreshable1)
        val fsmSubscription2 = processVariable.bind(refreshable2)

        coVerify { refreshable1.addFSMSubscription(any()) }
        coVerify { refreshable2.addFSMSubscription(any()) }
        coVerify { eventServiceDsl.onEvent(eventKey, callback = any()) }

        fsmSubscription1.cancel()
        coVerify(exactly = 0) { eventSubscription.cancel() }

        fsmSubscription2.cancel()
        coVerify { eventSubscription.cancel() }

        processVariable.bind(refreshable3)
        coVerify { refreshable3.addFSMSubscription(any()) }
        coVerify { eventServiceDsl.onEvent(eventKey, callback = any()) }
    }
}

package esw.ocs.dsl.epics

import akka.Done
import csw.params.events.Event
import csw.params.events.EventKey
import csw.params.events.EventName
import csw.params.events.SystemEvent
import csw.prefix.models.Prefix
import esw.ocs.dsl.highlevel.EventServiceDsl
import esw.ocs.dsl.highlevel.models.TCS
import esw.ocs.dsl.params.first
import esw.ocs.dsl.params.intKey
import io.kotlintest.shouldBe
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class ParamVariableTest {
    @Test
    fun `make should create ParamVariable and publish a initial event | ESW-291`() = runBlocking {
        TestSetup().run {
            coEvery { eventServiceDsl.getEvent(eventKeyStr) }.returns(mutableSetOf(systemEvent))
            coEvery { eventServiceDsl.publishEvent(any()) }.returns(Done.done())

            val intValue = 3
            val eventVariableImpl = ParamVariable.make(intValue, intKey, eventKey, eventServiceDsl)

            coEvery { eventServiceDsl.getEvent(eventKeyStr) }
            val eventSlot: CapturingSlot<Event> = slot()
            coVerify { eventServiceDsl.publishEvent(capture(eventSlot)) }
            eventSlot.captured.paramType().get(intKey).get().first shouldBe intValue

            eventVariableImpl.first() shouldBe intValue
        }
    }

    @Test
    fun `getParam should return the values of given Key from Parameters of Event | ESW-132, ESW-142, ESW-291`() = runBlocking {
        TestSetup().run {
            coEvery { eventServiceDsl.getEvent(eventKeyStr) }.returns(mutableSetOf(systemEvent))
            coEvery { eventServiceDsl.publishEvent(any()) }.returns(Done.done())
            val intValue = 6

            val eventVariableImpl = ParamVariable.make(intValue, intKey, eventKey, eventServiceDsl)
            eventVariableImpl.getParam() shouldBe intKey.set(intValue)
        }
    }

    @Test
    fun `first should return first value of given Key from Parameter of the Event | ESW-132, ESW-142, ESW-291`() = runBlocking {
        TestSetup().run {
            coEvery { eventServiceDsl.getEvent(eventKeyStr) }.returns(mutableSetOf(systemEvent))
            coEvery { eventServiceDsl.publishEvent(any()) }.returns(Done.done())

            val intValue = 10

            val eventVariableImpl = ParamVariable.make(intValue, intKey, eventKey, eventServiceDsl)

            eventVariableImpl.first() shouldBe intValue
        }
    }

    @Test
    fun `setParam should publish new event with the new value of the parameter | ESW-132, ESW-142`() = runBlocking {
        TestSetup().run {
            coEvery { eventServiceDsl.getEvent(eventKeyStr) }.returns(mutableSetOf(systemEvent))
            coEvery { eventServiceDsl.publishEvent(any()) }.returns(Done.done())

            val intValue = 5
            val paramVariable = ParamVariable.make(intValue, intKey, eventKey, eventServiceDsl)

            coEvery { eventServiceDsl.getEvent(eventKeyStr) }
            val eventSlot: CapturingSlot<Event> = slot()
            coVerify { eventServiceDsl.publishEvent(capture(eventSlot)) }
            eventSlot.captured.paramType().get(intKey).get().first shouldBe intValue

            val newValue = 100
            paramVariable.setParam(newValue)

            coVerify { eventServiceDsl.publishEvent(capture(eventSlot)) }
            eventSlot.captured.paramType().get(intKey).get().first shouldBe newValue // assert on the event which is being published
        }
    }

    private inner class TestSetup {
        val eventName = EventName("testEvent")
        val prefix = Prefix(TCS, "test")
        val intKey = intKey("testIntKey")

        val systemEvent = SystemEvent(prefix, eventName)
        val eventKey = EventKey(prefix, eventName)
        val eventKeyStr = eventKey.key()

        val eventServiceDsl = mockk<EventServiceDsl>()
    }
}

package esw.ocs.dsl.epics

import akka.Done
import csw.params.core.models.Prefix
import csw.params.events.EventName
import csw.params.events.SystemEvent
import esw.ocs.dsl.highlevel.EventServiceDsl
import esw.ocs.dsl.params.booleanKey
import esw.ocs.dsl.params.intKey
import esw.ocs.dsl.params.set
import io.kotlintest.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class ProcessVariableTest {
    @Test
    fun `set should update local value and publish new event | ESW-142`() = runBlocking {
        val prefix = Prefix("tcs")
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
        val systemEvent = SystemEvent(Prefix("tcs"), EventName("testEvent")).add(intKey.set(intValue))

        val processVariable: ProcessVariable<Int> = ProcessVariable(systemEvent, intKey, eventServiceDsl)

        processVariable.get() shouldBe intValue
    }
}
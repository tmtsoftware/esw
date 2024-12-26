package esw.ocs.dsl.epics

import org.apache.pekko.Done
import csw.params.events.Event
import csw.params.events.EventKey
import csw.params.events.EventName
import csw.params.events.SystemEvent
import csw.prefix.models.Prefix
import esw.ocs.dsl.highlevel.CswHighLevelDslApi
import esw.ocs.dsl.highlevel.models.TCS
import esw.ocs.dsl.params.first
import esw.ocs.dsl.params.intKey
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

@Suppress("DANGEROUS_CHARACTERS")
class ParamVariableTest {
    @Test
    fun `make_should_create_ParamVariable_and_publish_a_initial_event_|_ESW-291`() = runBlocking {
        TestSetup().run {
            coEvery { cswHighLevelDsl.getEvent(eventKeyStr) }.returns(systemEvent)
            coEvery { cswHighLevelDsl.publishEvent(any()) }.returns(Done.done())

            val intValue = 3
            val paramVariable = ParamVariable.make(intValue, intKey, eventKey, cswHighLevelDsl)

            coEvery { cswHighLevelDsl.getEvent(eventKeyStr) }
            val eventSlot: CapturingSlot<Event> = slot()
            coVerify { cswHighLevelDsl.publishEvent(capture(eventSlot)) }
            eventSlot.captured.paramType().get(intKey).get().first shouldBe intValue

            paramVariable.first() shouldBe intValue
        }
    }

    @Test
    fun `getParam_should_return_the_values_of_given_Key_from_Parameters_of_Event_|_ESW-132,_ESW-142,_ESW-291`() = runBlocking {
        TestSetup().run {
            coEvery { cswHighLevelDsl.getEvent(eventKeyStr) }.returns(systemEvent)
            coEvery { cswHighLevelDsl.publishEvent(any()) }.returns(Done.done())
            val initialValue = 6

            val paramVariable = ParamVariable.make(initialValue, intKey, eventKey, cswHighLevelDsl)
            paramVariable.getParam() shouldBe intKey.set(initialValue)
        }
    }

    @Test
    fun `first_should_return_first_value_of_given_Key_from_Parameter_of_the_Event_|_ESW-132,_ESW-142,_ESW-291`() = runBlocking {
        TestSetup().run {
            coEvery { cswHighLevelDsl.getEvent(eventKeyStr) }.returns(systemEvent)
            coEvery { cswHighLevelDsl.publishEvent(any()) }.returns(Done.done())

            val initialValue = 10

            val paramVariable = ParamVariable.make(initialValue, intKey, eventKey, cswHighLevelDsl)

            paramVariable.first() shouldBe initialValue
        }
    }

    @Test
    fun `setParam_should_publish_new_event_with_the_new_value_of_the_parameter_|_ESW-132,_ESW-142`() = runBlocking {
        TestSetup().run {
            coEvery { cswHighLevelDsl.getEvent(eventKeyStr) }.returns(systemEvent)
            coEvery { cswHighLevelDsl.publishEvent(any()) }.returns(Done.done())

            val initialValue = 5
            val paramVariable = ParamVariable.make(initialValue, intKey, eventKey, cswHighLevelDsl)

            coEvery { cswHighLevelDsl.getEvent(eventKeyStr) }
            val eventList: MutableList<Event> = mutableListOf<Event>()
            coVerify { cswHighLevelDsl.publishEvent(capture(eventList)) }
            eventList.first().paramType().get(intKey).get().first shouldBe initialValue

            val newValue = 100
            paramVariable.setParam(newValue)

            coVerify { cswHighLevelDsl.publishEvent(capture(eventList)) }
            eventList.last().paramType().get(intKey).get().first shouldBe newValue
        }
    }

    private inner class TestSetup {
        val eventName = EventName("testEvent")
        val prefix = Prefix(TCS, "test")
        val intKey = intKey("testIntKey")

        val systemEvent = SystemEvent(prefix, eventName)
        val eventKey = EventKey(prefix, eventName)
        val eventKeyStr = eventKey.key()

        val cswHighLevelDsl = mockk<CswHighLevelDslApi>()
    }
}

package esw.ocs.dsl.epics

import akka.Done
import csw.event.api.javadsl.IEventService
import csw.params.events.Event
import esw.ocs.dsl.compareTo
import esw.ocs.dsl.params.intKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

abstract class TestMachine(name: String, init: String) : Machine(name, init) {
    override val eventService: IEventService
        get() = TODO("not implemented") // To change initializer of created properties use File | Settings | File Templates.

    override suspend fun publishEvent(event: Event): Done {
        return Done.done()
    }

    override suspend fun getEvent(vararg eventKeys: String): Set<Event> {
        return setOf(Event.badEvent())
    }

    val prefix = "esw.epic"
    val tempKey = intKey("temp")
}

val machine1 = object : TestMachine("temp-monitor", "Init") {

    var temp = createVar(0, "$prefix.temp", tempKey)

    override suspend fun logic(state: String) {
        when (state) {
            "Init" -> {
                `when`() {
                    temp.set(45)
                    temp.pvPut()
                    become("Ok")
                }
            }

            "Ok" -> {
                `when`(temp.get() > 40) {
                    temp.set(25)
                    become("High")
                }
            }

            "High" -> {
                `when`(temp.get() < 30) {
                    //                    temp.pvGet()
                    become("Ok")
                }
            }
        }
    }

    override fun debugString(): String = "temp = $temp"
}

val machine2 = object : TestMachine("temp-monitor", "Init") {
    var temp by reactiveEvent(0, "$prefix.temp", tempKey) { _, o, n ->
        println("Temp changed from [$o to $n]")
    }

    override suspend fun logic(state: String) {
        when (state) {
            "Init" -> {
                `when`() {
                    temp = 45
                    become("Ok")
                }
            }

            "Ok" -> {
                `when`(temp > 40) {
                    temp = 25
                    become("High")
                }
            }

            "High" -> {
                `when`(temp < 30) {
                    become("Ok")
                }
            }
        }
    }

    override fun debugString(): String = "temp = $temp"
}

fun main() = runBlocking {
    machine2.refresh("Init")

    delay(10000)
}

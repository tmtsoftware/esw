package esw.ocs.scripts.examples.class_based

import esw.ocs.dsl.core.Script
import esw.ocs.dsl.script.CswServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Deprecated("Use script based approach to write scripts")
class Script1(cswServices: CswServices) : Script(cswServices) {
    init {
        val eventKey = "csw.a.b."
        fun event(id: Int) = SystemEvent("csw.a.b", id.toString())

        onSetup("command-1") {
            log("============ command-1 ================")

            repeat(50) {
                coroutineScope.launch {
                    log("Publishing event $it")
                    delay(1000)
                    publishEvent(event(it))
                    log("Published event $it")
                }
            }

            log("============ command-1 -End ================")
        }

        onSetup("command-2") {
            log("============ command-2 ================")
            val events = getEvent(eventKey + 1)
            log(events.toString())
            events.forEach(::println)

            log("============ command-2 End ================")
        }

        onSetup("command-3") {
            log("============ command-3 ================")

            val keys = (0.until(50)).map { eventKey + it }.toTypedArray()

            onEvent(*keys) { event ->
                println("=======================")
                log("Received: ${event.eventName()}")
            }

            log("============ command-3 End ================")
        }

        onShutdown {
            close()
        }
    }
}

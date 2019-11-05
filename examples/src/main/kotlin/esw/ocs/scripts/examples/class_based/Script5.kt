package esw.ocs.scripts.examples.class_based

import csw.params.core.models.Prefix
import csw.params.events.EventName
import csw.params.events.SystemEvent
import esw.ocs.dsl.core.Script
import esw.ocs.dsl.script.CswServices
import esw.ocs.scripts.examples.reusable_scripts.script6
import esw.ocs.scripts.examples.reusable_scripts.script7
import kotlinx.coroutines.delay

const val eventKey = "csw.a.b."
fun event(id: Int) = SystemEvent(Prefix("csw.a.b"), EventName(id.toString()))

@Deprecated("Use script based approach to write scripts")
class Script5(cswServices: CswServices) : Script(cswServices) {
    init {
        log("============= Loading script 5 ============")

        var totalEventsRec = 0

        loadScripts(
                script6,
                script7
        )

        handleSetup("command-3") {
            log("============ command-3 ================")

            val keys = (0.until(50)).map { eventKey + it }.toTypedArray()

            onEvent(*keys) { event ->
                println("=======================")
                log("Received: ${event.eventName()}")
                totalEventsRec += 1
            }

            log("============ command-3 End ================")
        }

        handleShutdown {
            while (totalEventsRec <= 49) {
                delay(100)
            }
            close()
        }
    }
}

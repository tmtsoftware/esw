package esw.ocs.dsl.scripts.class_based

import csw.params.commands.CommandResponse
import csw.params.core.models.Prefix
import csw.params.events.EventName
import csw.params.events.SystemEvent
import esw.ocs.dsl.CswServices
import esw.ocs.dsl.core.ScriptKt
import esw.ocs.dsl.scripts.reusable_scripts.script6
import esw.ocs.dsl.scripts.reusable_scripts.script7
import kotlinx.coroutines.delay

const val eventKey = "csw.a.b."
fun event(id: Int) = SystemEvent(Prefix("csw.a.b"), EventName(id.toString()))

class Script5(cswServices: CswServices) : ScriptKt(cswServices) {
    init {
        log("============= Loading script 5 ============")

        var totalEventsRec = 0

        loadScripts(
            script6,
            script7
        )

        handleSetup("command-3") { command ->
            log("============ command-3 ================")

            val keys = (0.until(50)).map { eventKey + it }.toTypedArray()

            onEvent(*keys) { event ->
                println("=======================")
                log("Received: ${event.eventName()}")
                totalEventsRec += 1
            }

            log("============ command-3 End ================")
            cswServices.crm().addOrUpdateCommand(CommandResponse.Completed(command.runId()))
        }

        handleShutdown {
            while (totalEventsRec <= 49) {
                delay(100)
            }
            close()
        }

    }

}
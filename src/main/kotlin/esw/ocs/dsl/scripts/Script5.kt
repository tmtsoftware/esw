package esw.ocs.dsl.scripts

import csw.params.commands.CommandResponse
import csw.params.core.models.Prefix
import csw.params.events.EventName
import csw.params.events.SystemEvent
import esw.ocs.dsl.CswServices
import esw.ocs.dsl.core.ScriptKt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val eventKey = "csw.a.b."
fun event(id: Int) = SystemEvent(Prefix("csw.a.b"), EventName(id.toString()))

class Script5(cswServices: CswServices) : ScriptKt(cswServices) {
    init {
        println("============= Loading script 5 ============")

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
            }

            log("============ command-3 End ================")
            cswServices.crm().addOrUpdateCommand(CommandResponse.Completed(command.runId()))
        }

        handleShutdown {
            close()
        }

    }

}
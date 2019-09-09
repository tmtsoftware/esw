package esw.ocs.dsl.scripts.script_based

import csw.params.commands.CommandResponse
import esw.ocs.dsl.core.script
import esw.ocs.dsl.scripts.reusable_scripts.script6
import esw.ocs.dsl.scripts.reusable_scripts.script7

script { csw ->

    val eventKey = "csw.a.b."

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
        csw.crm().addOrUpdateCommand(CommandResponse.Completed(command.runId()))
    }

    handleShutdown {
        close()
    }
}

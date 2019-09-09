package esw.ocs.dsl.scripts.reusable_scripts

import csw.params.commands.CommandResponse.Completed
import esw.ocs.dsl.core.reusableScript
import esw.ocs.dsl.scripts.class_based.eventKey

val script7 = reusableScript { cswServices ->
    log("============= Loading script 7 ============")

    handleSetup("command-2") { command ->
        log("============ command-2 ================")
        val events = getEvent(eventKey + 1)
        log(events.toString())
        events.forEach(::println)

        log("============ command-2 End ================")
        addOrUpdateCommand(Completed(command.runId()))
    }
}
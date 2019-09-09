package esw.ocs.dsl.scripts.class_based

import csw.params.commands.CommandResponse.Completed
import esw.ocs.dsl.core.reusableScript

val script7 = reusableScript { cswServices ->
    log("============= Loading script 7 ============")

    handleSetup("command-2") { command ->
        log("============ command-2 ================")
        val events = getEvent(eventKey + 1)
        log(events.toString())
        events.forEach(::println)

        log("============ command-2 End ================")
        cswServices.crm().addOrUpdateCommand(Completed(command.runId()))
    }
}
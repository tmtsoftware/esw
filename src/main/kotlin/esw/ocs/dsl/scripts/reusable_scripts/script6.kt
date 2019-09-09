package esw.ocs.dsl.scripts.reusable_scripts

import csw.params.commands.CommandResponse.Completed
import esw.ocs.dsl.core.reusableScript
import esw.ocs.dsl.scripts.class_based.event
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val script6 = reusableScript { cswServices ->

    log("============= Loading script 6 ============")

    handleSetup("command-1") { command ->
        log("============ command-1 ================")

        repeat(50) {
            launch {
                log("Publishing event $it")
                delay(1000)
                publishEvent(event(it))
                log("Published event $it")
            }
        }

        log("============ command-1 -End ================")
        addOrUpdateCommand(Completed(command.runId()))
    }

}
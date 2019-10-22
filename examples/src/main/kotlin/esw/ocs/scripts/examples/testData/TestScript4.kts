package esw.ocs.scripts.examples.testData

import csw.params.commands.CommandResponse
import esw.ocs.dsl.core.script
import kotlinx.coroutines.delay

script {

    handleSetup("command-irms") { command ->
        // To avoid sequencer to finish immediately so that other Add, Append command gets time
        delay(300)
        addOrUpdateCommand(CommandResponse.Completed(command.runId))
    }

    handleAbortSequence {
        //do some actions to abort sequence
        val successEvent = systemEvent("IRMS", "abort.success")
        publishEvent(successEvent)
    }

    handleStop {
        //do some actions to abort sequence
        val successEvent = systemEvent("IRMS", "stop.success")
        publishEvent(successEvent)
    }
}

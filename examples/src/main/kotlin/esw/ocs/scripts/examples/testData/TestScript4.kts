package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.script

script {

    handleSetup("command-irms") { _ ->
        // NOT update command response To avoid sequencer to
        // finish so that other commands gets time
    }

    handleAbortSequence {
        //do some actions to abort sequence
        val successEvent = systemEvent("tcs", "abort.success")
        publishEvent(successEvent)
    }

    handleStop {
        //do some actions to stop
        val successEvent = systemEvent("tcs", "stop.success")
        publishEvent(successEvent)
    }
}

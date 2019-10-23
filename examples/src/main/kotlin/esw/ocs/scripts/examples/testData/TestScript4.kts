package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.script

script {

    handleSetup("command-irms") { _ ->
        // To avoid sequencer to finish so that other commands gets time
    }

    handleAbortSequence {
        //do some actions to abort sequence
        println("abort irms")
        val successEvent = systemEvent("tcs", "abort.success")
        publishEvent(successEvent)
    }

    handleStop {
        //do some actions to stop
        println("stop irms")
        val successEvent = systemEvent("tcs", "stop.success")
        publishEvent(successEvent)
    }
}

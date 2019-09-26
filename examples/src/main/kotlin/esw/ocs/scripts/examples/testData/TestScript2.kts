package esw.ocs.scripts.examples.testData

import csw.params.commands.CommandResponse.Completed
import csw.params.commands.CommandResponse.Error
import esw.ocs.dsl.core.script
import esw.ocs.dsl.utils.booleanKey
import kotlinx.coroutines.delay

script {
    handleSetup("command-1") { command ->

        // To avoid sequencer to finish immediately so that other Add, Append command gets time
        delay(200)
        addOrUpdateCommand(Completed(command.runId))
    }

    handleSetup("command-2") { command ->

        addOrUpdateCommand(Completed(command.runId))
    }

    handleSetup("command-3") { command ->

        addOrUpdateCommand(Completed(command.runId))
    }

    handleSetup("fail-command") { command ->

        addOrUpdateCommand(Error(command.runId, command.commandName().name()))
    }

    handleGoOffline {

        // do some actions to go offline
        val param = booleanKey("offline").set(true)
        val event = systemEvent("TCS.test", "offline").add(param)
        publishEvent(event)
    }

    handleGoOnline {
        // do some actions to go online
        val param = booleanKey("online").set(true)
        val event = systemEvent("TCS.test", "online").add(param)
        publishEvent(event)
    }

    handleDiagnosticMode { startTime, hint ->
        // do some actions to go to diagnostic mode based on hint
        diagnosticModeForSequencer(
            "testSequencerId6", "testObservingMode6",
            startTime,
            hint
        )
    }

    handleOperationsMode {
        // do some actions to go to operations mode
        operationsModeForSequencer("testSequencerId6", "testObservingMode6")
    }
}

package esw.ocs.scripts.examples.testData

import csw.params.commands.CommandResponse.Completed
import csw.params.commands.CommandResponse.Error
import esw.ocs.dsl.core.reusableScript
import esw.ocs.dsl.core.script
import esw.ocs.dsl.params.set
import kotlinx.coroutines.delay

// ESW-134: Reuse code by ability to import logic from one script into another
val onlineOfflineHandlers = reusableScript {
    handleGoOffline {
        goOfflineModeForSequencer("testSequencerId6", "testObservingMode6")
        delay(1000)
    }

    handleGoOnline {
        goOnlineModeForSequencer("testSequencerId6", "testObservingMode6")
        delay(1000)
    }
}

// ESW-134: Reuse code by ability to import logic from one script into another
val operationsAndDiagModeHandlers = reusableScript {
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

script {
    handleSetup("command-1") { command ->

        // To avoid sequencer to finish immediately so that other Add, Append command gets time
        delay(200)
    }

    handleSetup("command-2") { command ->

    }

    handleSetup("command-3") { command ->

    }

    handleSetup("command-4") { command ->
        //Don't complete immediately as this is used to abort sequence usecase
        delay(700)
    }

    handleSetup("command-5") { command ->

    }

    handleSetup("command-6") { command ->

    }

    handleSetup("fail-command") { command ->
        finishWithError()
    }

    // ESW-134: Reuse code by ability to import logic from one script into another
    loadScripts(
        onlineOfflineHandlers,
        operationsAndDiagModeHandlers
    )
}

package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.script
import kotlinx.coroutines.delay

script {
    handleSetup("command-1") {
        // To avoid sequencer to finish immediately so that other Add, Append command gets time
        delay(200)
    }

    handleSetup("command-2") {
    }

    handleSetup("command-3") {
    }

    handleSetup("command-4") {
        //Don't complete immediately as this is used to abort sequence usecase
        delay(700)
    }

    handleSetup("command-5") {
    }

    handleSetup("command-6") {
    }

    handleSetup("fail-command") { command ->
        finishWithError(command.commandName().name())
    }

    // ESW-134: Reuse code by ability to import logic from one script into another
    loadScripts(
        InitialCommandHandler,
        OnlineOfflineHandlers,
        OperationsAndDiagModeHandlers
    )
}

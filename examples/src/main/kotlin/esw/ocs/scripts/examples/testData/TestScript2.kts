package esw.ocs.scripts.examples.testData

import csw.params.commands.CommandResponse.Completed
import csw.params.commands.CommandResponse.Error
import esw.ocs.dsl.core.script
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

    handleSetup("command-4") { command ->
        //Don't complete immediately as this is used to abort sequence usecase
        delay(700)
        addOrUpdateCommand(Completed(command.runId))
    }

    handleSetup("command-5") { command ->
        addOrUpdateCommand(Completed(command.runId))
    }

    handleSetup("command-6") { command ->
        addOrUpdateCommand(Completed(command.runId))
    }

    handleSetup("fail-command") { command ->
        addOrUpdateCommand(Error(command.runId, command.commandName().name()))
    }

    // ESW-134: Reuse code by ability to import logic from one script into another
    loadScripts(
        InitialCommandHandler,
        OnlineOfflineHandlers,
        OperationsAndDiagModeHandlers
    )
}

package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.script
import kotlinx.coroutines.delay
import kotlin.time.seconds

script {
    onSetup("command-1") {
        // To avoid sequencer to finish immediately so that other Add, Append command gets time
        delay(200)
    }

    onSetup("command-2") {
    }

    onSetup("command-3") {
    }

    onSetup("command-4") {
        //Don't complete immediately as this is used to abort sequence usecase
        delay(700)
    }

    onSetup("command-5") {
    }

    onSetup("command-6") {
    }

    onSetup("fail-command") { command ->
        finishWithError(command.commandName().name())
    }

    onSetup("multi-node") { command ->
        val sequence = sequenceOf(command)

        val tcs = Sequencer("tcs", "moonnight")
        tcs.submitAndWait(sequence, 10.seconds)
    }

    onSetup("log-command") {
        fatal("log-message")
    }

    // ESW-134: Reuse code by ability to import logic from one script into another
    loadScripts(
        InitialCommandHandler,
        OnlineOfflineHandlers,
        OperationsAndDiagModeHandlers
    )
}

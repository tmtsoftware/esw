package esw.ocs.scripts.examples.testData

import csw.params.commands.CommandResponse
import esw.ocs.dsl.core.reusableScript
import kotlinx.coroutines.delay

// ESW-134: Reuse code by ability to import logic from one script into another
val InitialCommandHandler = reusableScript {
    handleSetup("command-1") { command ->
        // To avoid sequencer to finish immediately so that other Add, Append command gets time
        delay(200)
    }
}
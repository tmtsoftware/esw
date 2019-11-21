package esw.ocs.scripts.examples.insights

import esw.ocs.dsl.core.script
import kotlinx.coroutines.delay

script {

    onSetup("command-1") { command ->
        // To avoid sequencer to finish immediately so that other Add, Append command gets time
        delay(200)
    }
}

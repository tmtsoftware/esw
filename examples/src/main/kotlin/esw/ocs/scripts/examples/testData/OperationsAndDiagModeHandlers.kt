package esw.ocs.scripts.examples.testData

import esw.ocs.api.models.ObsMode
import esw.ocs.dsl.core.reusableScript
import esw.ocs.dsl.highlevel.models.TCS
import kotlin.time.Duration
import kotlin.time.seconds

// ESW-134: Reuse code by ability to import logic from one script into another
val OperationsAndDiagModeHandlers = reusableScript {
    onDiagnosticMode { startTime, hint ->
        // do some actions to go to diagnostic mode based on hint
        val tcsSequencer = Sequencer(TCS, ObsMode("moonnight"), Duration.seconds(10))
        tcsSequencer.diagnosticMode(startTime, hint)
    }

    onOperationsMode {
        // do some actions to go to operations mode
        val tcsSequencer = Sequencer(TCS, ObsMode("moonnight"), Duration.seconds(10))
        tcsSequencer.operationsMode()
    }
}

package esw.ocs.scripts.examples.testData

import esw.ocs.api.models.ObsMode
import esw.ocs.dsl.core.reusableScript
import esw.ocs.dsl.highlevel.models.TCS
import kotlin.time.seconds

// ESW-134: Reuse code by ability to import logic from one script into another
val OperationsAndDiagModeHandlers = reusableScript {
    onDiagnosticMode { startTime, hint ->
        // do some actions to go to diagnostic mode based on hint
        val tcsSequencer = Sequencer(TCS, ObsMode("moonnight"), 10.seconds)
        tcsSequencer.diagnosticMode(startTime, hint)
    }

    onOperationsMode {
        // do some actions to go to operations mode
        val tcsSequencer = Sequencer(TCS, ObsMode("moonnight"), 10.seconds)
        tcsSequencer.operationsMode()
    }
}

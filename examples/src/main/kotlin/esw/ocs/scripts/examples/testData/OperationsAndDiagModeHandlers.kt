package esw.ocs.scripts.examples.testData

import csw.prefix.models.Prefix
import esw.ocs.dsl.core.reusableScript
import esw.ocs.dsl.highlevel.models.TCS
import kotlin.time.Duration

// ESW-134: Reuse code by ability to import logic from one script into another
val OperationsAndDiagModeHandlers = reusableScript {
    onDiagnosticMode { startTime, hint ->
        // do some actions to go to diagnostic mode based on hint
        val tcsSequencer = Sequencer(Prefix(TCS,"moonnight"), Duration.seconds(10))
        tcsSequencer.diagnosticMode(startTime, hint)
    }

    onOperationsMode {
        // do some actions to go to operations mode
        val tcsSequencer = Sequencer(Prefix(TCS,"moonnight"), Duration.seconds(10))
        tcsSequencer.operationsMode()
    }
}

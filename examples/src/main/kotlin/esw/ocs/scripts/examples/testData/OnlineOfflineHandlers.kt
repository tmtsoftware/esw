package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.reusableScript
import kotlinx.coroutines.delay

// ESW-134: Reuse code by ability to import logic from one script into another
val OnlineOfflineHandlers = reusableScript {
    handleGoOffline {
        val tcsSequencer = Sequencer("tcs", "moonnight")
        tcsSequencer.goOffline()
        delay(1000)
    }

    handleGoOnline {
        val tcsSequencer = Sequencer("tcs", "moonnight")
        tcsSequencer.goOnline()
        delay(1000)
    }
}
package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.reusableScript
import kotlinx.coroutines.delay

// ESW-134: Reuse code by ability to import logic from one script into another
val OnlineOfflineHandlers = reusableScript {
    onGoOffline {
        goOfflineModeForSequencer("tcs", "moonnight")
        delay(1000)
    }

    onGoOnline {
        goOnlineModeForSequencer("tcs", "moonnight")
        delay(1000)
    }
}
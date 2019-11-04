package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.reusableScript
import kotlinx.coroutines.delay

// ESW-134: Reuse code by ability to import logic from one script into another
val OnlineOfflineHandlers = reusableScript {
    handleGoOffline {
        goOfflineModeForSequencer("testSequencerId6", "testObservingMode6")
        delay(1000)
    }

    handleGoOnline {
        goOnlineModeForSequencer("testSequencerId6", "testObservingMode6")
        delay(1000)
    }
}
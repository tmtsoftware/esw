package esw.ocs.scripts.examples.testData

import esw.ocs.api.models.ObsMode
import esw.ocs.dsl.core.reusableScript
import esw.ocs.dsl.highlevel.models.TCS
import kotlinx.coroutines.delay
import kotlin.time.Duration

// ESW-134: Reuse code by ability to import logic from one script into another
val OnlineOfflineHandlers = reusableScript {
    onGoOffline {
        val tcsSequencer = Sequencer(TCS, ObsMode( "moonnight"), Duration.seconds(10))
        tcsSequencer.goOffline()
        delay(1000)
    }

    onGoOnline {
        val tcsSequencer = Sequencer(TCS, ObsMode("moonnight"), Duration.seconds(10))
        tcsSequencer.goOnline()
        delay(1000)
    }
}

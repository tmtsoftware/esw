package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.script
import kotlinx.coroutines.delay

script {
    onSetup("nonblocking-command") {
        delay(1200)
    }

    onSetup("blocking-command") {
        Thread.sleep(1200)
    }
}

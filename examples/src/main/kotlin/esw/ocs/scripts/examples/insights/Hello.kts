package esw.ocs.scripts.examples.insights

import esw.ocs.dsl.core.script
import kotlinx.coroutines.delay

script {

    onSetup("command-1") {
        delay(1000)
    }

    onSetup("command-2") {
        delay(1000)
    }

    onSetup("command-3") {
        delay(1000)
    }

    onSetup("command-4") {
        delay(1000)
    }

    onSetup("command-5") {
        delay(1000)
    }
}

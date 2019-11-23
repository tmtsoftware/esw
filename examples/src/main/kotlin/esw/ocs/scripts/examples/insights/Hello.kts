package esw.ocs.scripts.examples.insights

import esw.ocs.dsl.core.script
import kotlinx.coroutines.delay

script {
    for (x in 1..100)
        onSetup("command-$x"){
            delay(1000)
        }
}

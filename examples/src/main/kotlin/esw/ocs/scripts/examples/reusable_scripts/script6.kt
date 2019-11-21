package esw.ocs.scripts.examples.reusable_scripts

import esw.ocs.dsl.core.reusableScript
import esw.ocs.scripts.examples.class_based.event
import esw.ocs.scripts.examples.class_based.log
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val script6 = reusableScript {

    log("============= Loading script 6 ============")

    onSetup("command-1") {
        log("============ command-1 ================")

        repeat(50) {
            coroutineScope.launch {
                log("Publishing event $it")
                delay(1000)
                publishEvent(event(it))
                log("Published event $it")
            }
        }

        log("============ command-1 -End ================")
    }
}

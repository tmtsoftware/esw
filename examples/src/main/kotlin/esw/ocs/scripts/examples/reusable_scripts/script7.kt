package esw.ocs.scripts.examples.reusable_scripts

import esw.ocs.dsl.core.reusableScript
import esw.ocs.scripts.examples.class_based.eventKey

val script7 = reusableScript {
    log("============= Loading script 7 ============")

    handleSetup("command-2") {
        log("============ command-2 ================")
        val events = getEvent(eventKey + 1)
        log(events.toString())
        events.forEach(::println)

        log("============ command-2 End ================")
    }
}

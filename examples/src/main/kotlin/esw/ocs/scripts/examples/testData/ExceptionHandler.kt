package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.reusableScript

val exceptionHandlerScript = reusableScript {
    onException { exception ->
        val successEvent = systemEvent("tcs", exception.message + "")
        publishEvent(successEvent)
    }
}
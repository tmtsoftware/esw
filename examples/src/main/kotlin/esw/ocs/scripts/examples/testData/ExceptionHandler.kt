package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.reusableScript

val exceptionHandlerScript = reusableScript {
    handleException { exception ->
        val successEvent = SystemEvent("tcs", exception.message + "")
        publishEvent(successEvent)
    }
}
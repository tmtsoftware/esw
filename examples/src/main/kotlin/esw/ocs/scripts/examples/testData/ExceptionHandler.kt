package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.reusableScript

val exceptionHandlerScript = reusableScript {
    onException { exception ->
        val successEvent = SystemEvent("tcs", exception.message + "")
        publishEvent(successEvent)
    }
}
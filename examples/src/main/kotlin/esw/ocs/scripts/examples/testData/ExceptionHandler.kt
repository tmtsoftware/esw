package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.reusableScript

val exceptionHandlerScript = reusableScript {
    onGlobalError { exception ->
        val successEvent = SystemEvent("tcs.filter.wheel", exception.message + "")
        publishEvent(successEvent)
    }
}
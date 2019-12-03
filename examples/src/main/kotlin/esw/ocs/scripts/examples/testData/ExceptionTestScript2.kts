package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.script

script {

    loadScripts(exceptionHandlerScript)

    onSetup("error-handling") {
        throw RuntimeException("command-failed")
    }.onError {
        val errorEvent = SystemEvent("tcs", "onError-event")
        publishEvent(errorEvent)
    }.retry(2)

    onGoOffline {}

    onGoOnline {
        throw RuntimeException("handle-goOnline-failed")
    }
}
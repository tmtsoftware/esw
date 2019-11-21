package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.script

script {

    loadScripts(exceptionHandlerScript)

    onGoOffline {}

    onGoOnline {
        throw RuntimeException("handle-goOnline-failed")
    }
}
package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.script

script {

    loadScripts(exceptionHandlerScript)

    handleGoOffline {}

    handleGoOnline {
        throw RuntimeException("handle-goOnline-failed")
    }
}
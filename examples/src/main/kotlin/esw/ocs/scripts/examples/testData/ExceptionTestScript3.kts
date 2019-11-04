package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.script

script {

    loadScripts(exceptionHandlerScript)

    handleShutdown {
        throw RuntimeException("handle-shutdown-failed")
    }
}
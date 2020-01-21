package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.reusableScript
import esw.ocs.dsl.core.script

reusableScript {  }

script {

    loadScripts(exceptionHandlerScript)

    onShutdown {
        throw RuntimeException("handle-shutdown-failed")
    }
}

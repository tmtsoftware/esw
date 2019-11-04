package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.script
import kotlinx.coroutines.delay

script {

    loadScripts(exceptionHandlerScript)

    handleSetup("successful-command") {
        println("completed successfully")
    }

    handleSetup("long-running-setup") {
        delay(50000)
    }

    handleSetup("fail-setup") {
        throw RuntimeException("handle-setup-failed")
    }

    handleObserve("fail-observe") {
        throw RuntimeException("handle-observe-failed")
    }

    handleGoOffline {
        throw RuntimeException("handle-goOffline-failed")
    }

    handleShutdown {
        throw RuntimeException("handle-shutdown-failed")
    }

    handleDiagnosticMode { _, _ ->
        throw RuntimeException("handle-diagnostic-mode-failed")
    }

    handleOperationsMode {
        throw RuntimeException("handle-operations-mode-failed")
    }

    handleStop {
        throw RuntimeException("handle-stop-failed")
    }

    handleAbortSequence {
        throw RuntimeException("handle-abort-failed")
    }
}
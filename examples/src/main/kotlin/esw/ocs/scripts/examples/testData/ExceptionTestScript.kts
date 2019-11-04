package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.script
import kotlinx.coroutines.delay

script {

    onException { exception ->
        val successEvent = systemEvent("tcs", exception.message + "")
        publishEvent(successEvent)
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

    handleDiagnosticMode { time, hint ->
        throw RuntimeException("handle-diagnostic-mode-failed")
    }

    handleOperationsMode {
        throw RuntimeException("handle-operations-mode-failed")
    }

    handleSetup("long-running-setup") {
        delay(50000)
    }

    handleStop {
        throw RuntimeException("handle-stop-failed")
    }

    handleAbortSequence {
        throw RuntimeException("handle-abort-failed")
    }

    handleSetup("successful-command") {
        println("completed successfully")
    }
}
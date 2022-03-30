/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.script
import kotlinx.coroutines.delay

script {

    loadScripts(exceptionHandlerScript)

    onSetup("successful-command") {
        println("completed successfully")
    }

    onSetup("long-running-setup") {
        delay(50000)
    }

    onSetup("fail-setup") {
        throw RuntimeException("handle-setup-failed")
    }

    onObserve("fail-observe") {
        throw RuntimeException("handle-observe-failed")
    }

    onGoOffline {
        throw RuntimeException("handle-goOffline-failed")
    }

    onDiagnosticMode { _, _ ->
        throw RuntimeException("handle-diagnostic-mode-failed")
    }

    onOperationsMode {
        throw RuntimeException("handle-operations-mode-failed")
    }

    onStop {
        throw RuntimeException("handle-stop-failed")
    }

    onAbortSequence {
        throw RuntimeException("handle-abort-failed")
    }
}
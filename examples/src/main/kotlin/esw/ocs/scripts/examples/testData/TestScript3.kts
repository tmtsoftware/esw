package esw.ocs.scripts.examples.testData

import csw.params.core.models.Prefix
import csw.params.events.EventName
import csw.params.events.SystemEvent
import esw.ocs.dsl.core.script
import esw.ocs.dsl.params.set
import esw.ocs.dsl.params.stringKey

script {

    handleDiagnosticMode { _, _ ->
        // do some actions to go to diagnostic mode based on hint
        val diagnosticModeParam = stringKey("mode").set("diagnostic")
        val event = systemEvent("tcs.test", "diagnostic-data", diagnosticModeParam)
        publishEvent(event)
    }

    handleOperationsMode {
        // do some actions to go to operations mode
        val operationsModeParam = stringKey("mode").set("operations")
        val event = systemEvent("tcs.test", "diagnostic-data", operationsModeParam)
        publishEvent(event)
    }

    handleGoOnline {
        val onlineParam = stringKey("mode").set("online")
        val event = systemEvent("tcs.test", "online", onlineParam)
        publishEvent(event)
    }

    handleGoOffline {
        val offlineParam = stringKey("mode").set("offline")
        val event = systemEvent("tcs.test", "offline", offlineParam)
        publishEvent(event)
    }
}

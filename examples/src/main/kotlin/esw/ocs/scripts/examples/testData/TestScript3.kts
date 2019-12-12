package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.script
import esw.ocs.dsl.params.stringKey

script {

    onDiagnosticMode { _, _ ->
        // do some actions to go to diagnostic mode based on hint
        val diagnosticModeParam = stringKey("mode").set("diagnostic")
        val event = SystemEvent("tcs.test", "diagnostic-data", diagnosticModeParam)
        publishEvent(event)
    }

    onOperationsMode {
        // do some actions to go to operations mode
        val operationsModeParam = stringKey("mode").set("operations")
        val event = SystemEvent("tcs.test", "diagnostic-data", operationsModeParam)
        publishEvent(event)
    }

    onGoOnline {
        val onlineParam = stringKey("mode").set("online")
        val event = SystemEvent("tcs.test", "online", onlineParam)
        publishEvent(event)
    }

    onGoOffline {
        val offlineParam = stringKey("mode").set("offline")
        val event = SystemEvent("tcs.test", "offline", offlineParam)
        publishEvent(event)
    }


    onSetup("multi-node") { command ->
        val assembly = Assembly("esw.SampleAssembly")
        assembly.submit(command)
    }
}

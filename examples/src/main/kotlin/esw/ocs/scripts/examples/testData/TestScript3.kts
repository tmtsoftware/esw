package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.script
import esw.ocs.dsl.highlevel.models.ESW
import esw.ocs.dsl.params.stringKey
import kotlin.time.seconds

script {

    onDiagnosticMode { _, _ ->
        // do some actions to go to diagnostic mode based on hint
        val diagnosticModeParam = stringKey("mode").set("diagnostic")
        val event = SystemEvent("TCS.test", "diagnostic-data", diagnosticModeParam)
        publishEvent(event)
    }

    onOperationsMode {
        // do some actions to go to operations mode
        val operationsModeParam = stringKey("mode").set("operations")
        val event = SystemEvent("TCS.test", "diagnostic-data", operationsModeParam)
        publishEvent(event)
    }

    onGoOnline {
        val onlineParam = stringKey("mode").set("online")
        val event = SystemEvent("TCS.test", "online", onlineParam)
        publishEvent(event)
    }

    onGoOffline {
        val offlineParam = stringKey("mode").set("offline")
        val event = SystemEvent("TCS.test", "offline", offlineParam)
        publishEvent(event)
    }


    onSetup("multi-node") { command ->
        val assembly = Assembly(ESW, "SampleAssembly", 10.seconds)
        assembly.submit(command)
    }
}

package esw.ocs.scripts.examples.testData

import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.ocs.dsl.core.script
import esw.ocs.dsl.params.stringKey
import scala.jdk.javaapi.CollectionConverters
import java.util.*

script {

    handleDiagnosticMode { _, _ ->
        // do some actions to go to diagnostic mode based on hint
        val diagnosticModeParam = stringKey("mode").set("diagnostic")
        val event = SystemEvent("tcs.test", "diagnostic-data", diagnosticModeParam)
        publishEvent(event)
    }

    handleOperationsMode {
        // do some actions to go to operations mode
        val operationsModeParam = stringKey("mode").set("operations")
        val event = SystemEvent("tcs.test", "diagnostic-data", operationsModeParam)
        publishEvent(event)
    }

    handleGoOnline {
        val onlineParam = stringKey("mode").set("online")
        val event = SystemEvent("tcs.test", "online", onlineParam)
        publishEvent(event)
    }

    handleGoOffline {
        val offlineParam = stringKey("mode").set("offline")
        val event = SystemEvent("tcs.test", "offline", offlineParam)
        publishEvent(event)
    }

    handleSetup("multi-node") {command ->
        submitCommandToAssembly("SampleAssembly", command)
    }
}

package esw.ocs.scripts.examples.testData

import csw.params.core.models.Prefix
import csw.params.events.EventName
import csw.params.events.SystemEvent
import esw.ocs.dsl.core.script
import esw.ocs.dsl.utils.stringKey

script {

    handleDiagnosticMode { startTime, hint ->
        //todo: try to remove match case
        // do some actions to go to diagnostic mode based on hint
        val diagnosticModeParam = stringKey("mode").set("diagnostic")
        val event = SystemEvent(Prefix("tcs.test"), EventName("diagnostic-data")).add(diagnosticModeParam)
        publishEvent(event)
    }

    handleOperationsMode {
        // do some actions to go to operations mode
        val operationsModeParam = stringKey("mode").set("operations")
        val event = SystemEvent(Prefix("tcs.test"), EventName("diagnostic-data")).add(operationsModeParam)
        publishEvent(event)
    }
}

package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.script

script {
//
//    handleDiagnosticMode {
//        //todo: try to remove match case
//        case(startTime, hint) =>
//        spawn {
//            // do some actions to go to diagnostic mode based on hint
//            val diagnosticModeParam = StringKey.make("mode").set("diagnostic")
//            val event = SystemEvent(Prefix("tcs.test"), EventName("diagnostic-data")).add(diagnosticModeParam)
//            csw.publishEvent(event).toScala.await
//        }
//    }
//
//    handleOperationsMode {
//        spawn {
//            // do some actions to go to operations mode
//            val operationsModeParam = StringKey.make("mode").set("operations")
//            val event = SystemEvent(Prefix("tcs.test"), EventName("diagnostic-data")).add(operationsModeParam)
//            csw.publishEvent(event).toScala.await
//        }
//    }
}

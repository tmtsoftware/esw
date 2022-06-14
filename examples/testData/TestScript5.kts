package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.script

script {

    onSetup("command-1") {
        publishEvent(SystemEvent("ESW.IRIS_cal", "event-1"))
    }
}
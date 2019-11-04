package esw.ocs.scripts.examples.testData

import csw.params.commands.CommandResponse
import esw.ocs.dsl.core.script


script {

    onException { exception ->
        val successEvent = systemEvent("tcs", exception.message + "")
        publishEvent(successEvent)
    }

    handleSetup("fail-setup") {
        throw RuntimeException("setup-failed")
    }

    handleSetup("next-command") { command ->
    }

}
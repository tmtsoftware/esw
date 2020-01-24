package esw.ocs.scripts.examples.testData

import csw.params.commands.CommandResponse.SubmitResponse
import esw.ocs.dsl.core.script
import esw.ocs.dsl.highlevel.RichComponent
import esw.ocs.dsl.highlevel.models.ESW
import esw.ocs.dsl.onFailed
import kotlin.time.seconds

script {

    loadScripts(exceptionHandlerScript)

    onSetup("error-handling") { command ->

        val hcd = Hcd(ESW, "testHcd", 10.seconds)
        hcd.submitAndWait(command)

    }.onError {

        val errorEvent = SystemEvent("TCS.filter.wheel", "onError-event")
        publishEvent(errorEvent)

    }.retry(2)

    onSetup("negative-submit-response") { command ->

        val hcd: RichComponent = Hcd(ESW, "testHcd", 10.seconds)
        val submitResponse: SubmitResponse = hcd.submitAndWait(command, resumeOnError = true)

        submitResponse.onFailed {
            val negativeResponseEvent = SystemEvent("TCS.filter.wheel", "negative-response-error")
            publishEvent(negativeResponseEvent)
        }

    }.onError {
        val errorEvent = SystemEvent("TCS.filter.wheel", "onError-event")
        publishEvent(errorEvent)
    }

    onGoOffline {}

    onGoOnline {
        throw RuntimeException("handle-goOnline-failed")
    }
}
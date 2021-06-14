@file:Suppress("UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox

import csw.params.commands.CommandResponse.*
import esw.ocs.dsl.*
import esw.ocs.dsl.core.script
import esw.ocs.dsl.highlevel.models.IRIS
import kotlin.time.Duration
import kotlin.time.seconds

script {
    val assembly = Assembly(IRIS, "filter.wheel", Duration.seconds(5))

    onSetup("submit-error-handling") { command ->
        // #extensions
        val positiveSubmitResponse: SubmitResponse = assembly.submit(command)

        positiveSubmitResponse
                .onStarted { startedResponse ->
                    val finalResponse: SubmitResponse = assembly.queryFinal(startedResponse.runId())
                    info("command completed with result: $finalResponse")
                }
                .onCompleted { completedResponse ->
                    info("command with ${completedResponse.runId()} is completed with result: ${completedResponse.result}")
                }
        // #extensions


        // #kotlin-when
        when (positiveSubmitResponse) {
            is Started -> {
                val finalResponse: SubmitResponse = assembly.queryFinal(positiveSubmitResponse.runId())
                info("command completed with response: $finalResponse")
            }
            is Completed -> info("command with ${positiveSubmitResponse.runId()} is completed")
        }
        // #kotlin-when


        // #onFailed
        val submitResponse: SubmitResponse = assembly.submit(command, resumeOnError = true)
        positiveSubmitResponse.onFailed { failedResponse ->
            error("command completed with result: $failedResponse")
        }
        // #onFailed


        // #onFailedTerminate
        val submitRes: SubmitResponse = assembly.submit(command, resumeOnError = true)
        positiveSubmitResponse.onFailedTerminate()
        // #onFailedTerminate

    }
}

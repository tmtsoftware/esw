package esw.ocs.scripts.examples.dsl

import csw.params.commands.CommandResponse.*
import csw.params.commands.Result
import esw.ocs.dsl.core.script
import esw.ocs.dsl.onCompleted
import esw.ocs.dsl.onStarted
import esw.ocs.dsl.orElse
import esw.ocs.dsl.result
import kotlin.time.hours
import kotlin.time.seconds

script {
    val assembly = Assembly("esw.filter.wheel")

    onSetup("default-submitAndWait-error-handling") { command ->

        // If submitAndWait returns negative response (which is considered as error by default)
        // then current execution flow breaks and onError command handler gets invoked
        // submitAndWait always returns final response
        val submitResponseResult: Result = assembly.submitAndWait(command, 2.hours).result

        info("${submitResponseResult.paramSet()}")

    }.onError { err ->
        error(err.reason)
    }

    onSetup("default-submit-error-handling") { command ->

        // if submit returns negative response (which is considered as error by default)
        // then current execution flow breaks and onError command handler gets invoked
        val submitResponse: SubmitResponse = assembly.submit(command)

        //  First approach - using custom dsl (this is an alternative to kotlin pattern match using when)
        submitResponse
                .onStarted { startedRes ->
                    val completedResponse = assembly.queryFinal(startedRes.runId(), 10.seconds)
                    info("command completed with result: ${completedResponse.result}")
                }
                .onCompleted { completed ->
                    info("command with ${completed.runId()} is completed with result: ${completed.result}")
                }

        // Second approach - using kotlin pattern matching
        when (submitResponse) {
            is Started -> {
                val completedResponse = assembly.queryFinal(submitResponse.runId(), 10.seconds)
                info("command completed with response: $completedResponse")
            }

            is Completed -> info("command with ${submitResponse.runId()} is completed")

            else -> finishWithError("Error starting WFS exposures: $submitResponse")
        }

    }.onError { err ->
        error(err.reason)
    }

    onSetup("resumeOnError-submit-error-handling") { command ->

        // if submit returns negative response
        // then current execution flow will continue because resumeOnError = true
        val submitResponse: SubmitResponse = assembly.submit(command, resumeOnError = true)

        //  First approach - using custom dsl (this is an alternative to kotlin pattern match using when)
        submitResponse
                .onStarted { startedRes ->
                    val completedResponse = assembly.queryFinal(startedRes.runId(), 10.seconds)
                    info("command completed with result: ${completedResponse.result}")
                }
                .onCompleted { completed ->
                    info("command with ${completed.runId()} is completed with result: ${completed.result}")
                }
                .orElse { response ->
                    finishWithError("Error starting WFS exposures: $response")
                }

    }.onError { err ->
        error(err.reason)
    }


    onGlobalError { err ->
        // cleanup global resources
        error(err.reason)
    }

}
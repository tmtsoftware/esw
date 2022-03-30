/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.scripts.examples.dsl

import csw.params.commands.CommandResponse.*
import csw.params.commands.Result
import esw.ocs.dsl.*
import esw.ocs.dsl.core.script
import esw.ocs.dsl.highlevel.models.ESW
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

fun takeExposure(): Unit = println("Taking exposure")
fun processResult(result: Result): Unit = println("Processing $result")

script {
    val assembly = Assembly(ESW, "filter.wheel", 10.seconds)

    onSetup("submitAndWait-error-handling") { command ->

        /* =========== Scenario-1 (default) ============
         * If submitAndWait returns negative response (which is considered as error by default)
         * then current execution flow breaks and onError command handler gets invoked
         * submitAndWait always returns final successful response in this case otherwise fails and invokes onError handler
         */
        val submitResult: Result = assembly.submitAndWait(command, 2.hours).result
        processResult(submitResult)

        /* =========== Scenario-2 (resumeOnError = true)============
         * If submitAndWait returns negative response
         * then current execution flow will continue because resumeOnError = true
         * In this case, submitAndWait can return final response but not necessarily successful response
         */
        val submitResponse: SubmitResponse = assembly.submitAndWait(command, 2.hours, resumeOnError = true)
        submitResponse
                .onFailed {
                    error("Negative SubmitResponse $it")
                    // do some corrective actions / send message to operator ...
                }
                .onCompleted {
                    takeExposure()
                }
    }.onError { err ->
        error(err.reason)
    }

    onSetup("submit-error-handling") { command ->

        /* =========== Scenario-1 (default) ============
         * if submit returns negative response (which is considered as error by default)
         * then current execution flow breaks and onError command handler gets invoked
         * Hence, only Started (in case of long-running command) or Completed (in case of short running command) response is returned
         */
        val positiveSubmitResponse: SubmitResponse = assembly.submit(command)

        //  First approach - using custom dsl (this is an alternative to kotlin pattern match using when)
        positiveSubmitResponse
                .onStarted { startedRes ->
                    val completedResponse = assembly.queryFinal(startedRes.runId())
                    info("command completed with result: ${completedResponse.result}")
                }
                .onCompleted { completed ->
                    info("command with ${completed.runId()} is completed with result: ${completed.result}")
                }

        // Second approach - using kotlin pattern matching
        when (positiveSubmitResponse) {
            is Started -> {
                val completedResponse = assembly.queryFinal(positiveSubmitResponse.runId())
                info("command completed with response: $completedResponse")
            }
            is Completed -> info("command with ${positiveSubmitResponse.runId()} is completed")
            else -> finishWithError("Error starting WFS exposures: $positiveSubmitResponse")
        }

        /* =========== Scenario-2 (resumeOnError = true) ============
         * if submit returns negative response
         * then current execution flow will continue because resumeOnError = true
         * Here, all the possible SubmitResponses are expected to be returned
         */
        val submitResponse: SubmitResponse = assembly.submit(command, resumeOnError = true)

        //  First approach - using custom dsl (this is an alternative to kotlin pattern match using when)
        submitResponse
                .onStarted { startedRes ->
                    val completedResponse = assembly.queryFinal(startedRes.runId())
                    info("command completed with result: ${completedResponse.result}")
                }
                .onCompleted { completed ->
                    info("command with ${completed.runId()} is completed with result: ${completed.result}")
                }
                .onFailedTerminate()

    }.onError { err ->
        error(err.reason)
    }

    onGlobalError { err ->
        // cleanup global resources
        error(err.reason)
    }
}
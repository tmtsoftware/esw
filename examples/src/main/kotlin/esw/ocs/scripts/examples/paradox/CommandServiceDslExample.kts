@file:Suppress("UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox

import csw.params.commands.CommandResponse
import csw.params.core.states.StateName
import esw.ocs.dsl.*
import esw.ocs.dsl.core.script
import esw.ocs.dsl.params.intKey
import kotlin.time.seconds

script {

    // #assembly
    val galilAssembly = Assembly("TCS.galil", defaultTimeout = 10.seconds)
    // #assembly

    // #hcd
    val filterWheelHcd = Hcd("TCS.filter.wheel.hcd", defaultTimeout = 10.seconds)
    // #hcd

    onSetup("setup-filter-assembly") { command ->
        // #lock-component
        galilAssembly.lock(
                leaseDuration = 20.seconds,
                onLockAboutToExpire = {
                    // do something when lock is about to expire
                    publishEvent(SystemEvent("ESW.test", "TCS.lock.about.to.expire"))
                },
                onLockExpired = {
                    // do something when lock expired
                    publishEvent(SystemEvent("ESW.test", "TCS.lock.expired"))
                }
        )
        // #lock-component

        // #unlock-component
        galilAssembly.unlock()
        // #unlock-component

        // #submit-and-wait-component
        // #submit-component
        val parameters = intKey("target").set(100)
        val galilCommand = Setup("ESW.iris_darkNight", "moveWheel", command.obsId).add(parameters)
        // #submit-component
        galilAssembly.submitAndWait(galilCommand, timeout = 20.seconds)
        // #submit-and-wait-component

        // #query-component
        val response = galilAssembly.submit(command)
        val queryResponse = galilAssembly.query(response.runId())
        // #query-component

        // #query-final-component
        // #submit-component
        val startedResponse = galilAssembly.submit(galilCommand)
        // #submit-component
        val finalResponse = galilAssembly.queryFinal(startedResponse.runId())
        // #query-final-component

        // #subscribe-current-state-component
        galilAssembly.subscribeCurrentState(StateName("stateName1")) { currentState ->
            // do something with currentState matching provided state name
            println("current state : $currentState")
        }
        // #subscribe-current-state-component
    }

    // #submit-component-on-error
    onSetup("submit-error-handling") { command ->

        /* =========== Scenario-1 (default) ============
         * if submit returns negative response (which is considered as error by default)
         * then current execution flow breaks and onError command handler gets invoked
         * Hence, only Started (in case of long-running command) or Completed (in case of short running command) response is returned
         */
        val parameters = intKey("target").set(100)
        val galilCommand = Setup("ESW.iris_darkNight", "moveWheel", command.obsId).add(parameters)
        val positiveSubmitResponse: CommandResponse.SubmitResponse = galilAssembly.submit(galilCommand)

        //  First approach - using custom dsl (this is an alternative to kotlin pattern match using when)
        positiveSubmitResponse
                .onStarted { startedRes ->
                    val completedResponse = galilAssembly.queryFinal(startedRes.runId())
                    info("command completed with result: ${completedResponse.result}")
                }
                .onCompleted { completed ->
                    info("command with ${completed.runId()} is completed with result: ${completed.result}")
                }

        // Second approach - using kotlin pattern matching
        when (positiveSubmitResponse) {
            is CommandResponse.Started -> {
                val completedResponse = galilAssembly.queryFinal(positiveSubmitResponse.runId())
                info("command completed with response: $completedResponse")
            }
            is CommandResponse.Completed -> info("command with ${positiveSubmitResponse.runId()} is completed")
        }

        // Third approach - use the isStarted value
        if (positiveSubmitResponse.isStarted) {
            val completedResponse = galilAssembly.queryFinal(positiveSubmitResponse.runId())
            info("command completed with result: ${completedResponse.result}")
        } else {
            info("command with ${positiveSubmitResponse.runId()} is completed with result: ${positiveSubmitResponse.result}")
        }

    }.onError { err ->
        // onError is called when submit command to galil assembly fails
        error(err.reason)
    }
    // #submit-component-on-error

    // #submit-component-error-resume
    onSetup("submit-error-handling-resume") { command ->
        /* =========== Scenario-2 (resumeOnError = true) ============
         * if submit returns negative response
         * then current execution flow will continue because resumeOnError = true
         * Here, all the possible SubmitResponses are expected to be returned
         */
        val parameters = intKey("target").set(100)
        val galilCommand = Setup("ESW.iris_darkNight", "moveWheel", command.obsId).add(parameters)
        val submitResponse: CommandResponse.SubmitResponse = galilAssembly.submit(galilCommand, resumeOnError = true)

        //  First approach - using custom dsl (this is an alternative to kotlin pattern match using when)
        submitResponse
                .onStarted { startedRes ->
                    val completedResponse = galilAssembly.queryFinal(startedRes.runId())
                    info("command completed with result: ${completedResponse.result}")
                }
                .onCompleted { completed ->
                    info("command with ${completed.runId()} is completed with result: ${completed.result}")
                }
                .onFailed { negativeResponse ->
                    error("command with ${negativeResponse.runId()} is failed with result: ${negativeResponse}")

                }

        // Script writer can still choose to terminate sequence in case of negative response
        submitResponse.onFailedTerminate()
    }
    // #submit-component-error-resume

    // #diagnostic-mode-component
    onDiagnosticMode { startTime, hint ->
        // do some actions to go to diagnostic mode based on hint
        galilAssembly.diagnosticMode(startTime, hint)
    }
    // #diagnostic-mode-component

    // #operations-mode-component
    onOperationsMode {
        // do some actions to go to operations mode
        galilAssembly.operationsMode()
    }
    // #operations-mode-component

    // #goOffline-component
    onGoOffline {
        // do some actions to go offline

        galilAssembly.goOffline()

    }
    // #goOffline-component

    // #goOnline-component
    onGoOnline {
        // do some actions to go online
        galilAssembly.goOnline()
    }
    // #goOnline-component
}


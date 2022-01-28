@file:Suppress("UNUSED_VARIABLE", "DIVISION_BY_ZERO", "UNUSED_ANONYMOUS_PARAMETER")

package esw.ocs.scripts.examples.paradox

import csw.params.commands.CommandResponse.SubmitResponse
import csw.time.scheduler.api.Cancellable
import esw.ocs.dsl.core.script
import esw.ocs.dsl.highlevel.models.IRIS
import esw.ocs.dsl.highlevel.models.WFOS
import esw.ocs.dsl.par
import esw.ocs.dsl.params.floatKey
import esw.ocs.dsl.params.invoke
import esw.ocs.dsl.params.stringKey
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.milliseconds

script {

    // #diagnosticMode
    var diagnosticEventCancellable: Cancellable? = null

    // #diagnosticMode
    val assembly = Assembly(IRIS, "filter.wheel", 5.seconds)

    // #onSetup
    onSetup("setupInstrument") {command ->
        // split command and send to downstream
        val assembly1 = Assembly(WFOS, "filter.blueWheel", 5.seconds)
        val assembly2 = Assembly(WFOS, "filter.redWheel", 5.seconds)
        par(
                { assembly1.submit(Setup("WFOS.wfos_darknight", "move")) },
                { assembly2.submit(Setup("WFOS.wfos_darknight", "move")) }
        )
    }
    // #onSetup


    // #onObserve
    // A detector assembly is defined with a long timeout of 60 minutes
    val detectorAssembly = Assembly(WFOS, "detectorAssembly", 60.minutes)
    val exposureKey = floatKey("exposureTime")

    onObserve("startExposure") { observe ->
        // Extract the input exposure time and send a startObserve command to the detector Assembly
        val expsosureTime = observe(exposureKey).head()
        detectorAssembly.submitAndWait(Setup("WFOS.sequencer", "startObserve", observe.obsId).add(observe(exposureKey)))
    }
    // #onObserve

    // #onGoOnline
    onGoOnline {
        // send command to downstream components
        assembly.goOnline()
    }
    // #onGoOnline

    // #onGoOffline
    onGoOffline {
        // send command to downstream components
        assembly.goOffline()
    }
    // #onGoOffline

    // #abort
    onAbortSequence {
        // cleanup steps to be done before aborting will go here
    }
    // #abort

    // #stop
    onStop {
        // steps for clearing sequencer-state before stopping will go here
    }
    // #stop

    // #shutdown
    onShutdown {
        // cleanup steps to be done before shutdown will go here
    }
    // #shutdown

    // #diagnosticMode
    onDiagnosticMode { startTime, hint ->
        // start publishing diagnostic data on a supported hint (for e.g. engineering)
        when (hint) {
            "engineering" -> {
                val diagnosticEvent = SystemEvent("ESW.ESW_darknight", "diagnostic")
                diagnosticEventCancellable = schedulePeriodically(startTime, 50.milliseconds) {
                    publishEvent(diagnosticEvent)
                }
            }
        }
    }
    // #diagnosticMode

    // #operationsMode
    onOperationsMode {
        // cancel all publishing events done from diagnostic mode
        diagnosticEventCancellable?.cancel()
        // send operations command to downstream
        assembly.operationsMode()
    }
    // #operationsMode

    // #onGlobalError
    // Scenario-1 onObserve handler fails
    onObserve("trigger-filter-wheel") { command ->
        val triggerStartEvent = SystemEvent("esw.command", "trigger.start", command(stringKey(name = "triggerTime")))
        // publishEvent fails with EventServerNotAvailable which fails onObserve handler
        // onGlobalError handler is called
        // Sequence is terminated with failure.
        publishEvent(triggerStartEvent)
    }

    // Scenario-2 onSetup handler fails - submit returns negative SubmitResponse
    onSetup("command-2") { command ->
        val assembly1 = Assembly(IRIS, "filter.wheel", 5.seconds)

        // Submit command to assembly return negative response. (error by default) onGlobalError handler is called.
        // Sequence is terminated with failure.
        assembly1.submit(command)
    }

    // Scenario-3
    onDiagnosticMode { startTime, hint ->
        // publishEvent fails with EventServerNotAvailable
        // onDiagnosticMode handler fails
        // onGlobalError is called. Sequence execution continues.
        publishEvent(SystemEvent("esw.diagnostic.mode", hint))
    }

    onGlobalError { error ->
        val errorReason = stringKey("reason").set(error.reason)
        val errorEvent = SystemEvent("esw.observation.end", "error", errorReason)
        publishEvent(errorEvent)
    }
    // #onGlobalError

    // #onError-for-exception
    onSetup("submit-error-handling") { command ->
        // some logic that results into a Runtime exception
        val result: Int = 1 / 0
    }.onError { err ->
        error(err.reason)
    }
    // #onError-for-exception

    // #onError-for-negative-response
    onSetup("submit-error-handling") { command ->
        val positiveSubmitResponse: SubmitResponse = assembly.submit(command)

    }.onError { err ->
        // onError is called when submit command to the assembly fails with a negative response (error, invalid etc)
        error(err.reason)
    }
    // #onError-for-negative-response

    // #retry
    onSetup("submit-error-handling") { command ->
        val assembly1 = Assembly(IRIS, "filter.wheel", 5.seconds)

        // Submit command to assembly return negative response. - error by default
        assembly1.submit(command)
    }.onError { err ->
        error(err.reason)
    }.retry(2)
    // #retry

    // #retry-with-interval
    onSetup("submit-error-handling") { command ->
        val assembly1 = Assembly(IRIS, "filter.wheel", 5.seconds)

        // Submit command to assembly return negative response. - error by default
        assembly1.submit(command)
    }.retry(2, 10.seconds)
    // #retry-with-interval

}

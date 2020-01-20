@file:Suppress("UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox

import csw.params.events.SystemEvent
import csw.time.scheduler.api.Cancellable
import esw.ocs.dsl.core.script
import esw.ocs.dsl.par
import esw.ocs.dsl.params.invoke
import esw.ocs.dsl.params.stringKey
import kotlin.time.milliseconds
import kotlin.time.seconds

script {

    var diagnosticEventCancellable: Cancellable? = null
    val assembly = Assembly("filter.wheel", 5.seconds)

    // #onSetup
    onSetup("command1") {
        // split command and send to downstream
        val assembly1 = Assembly("filter.wheel", 5.seconds)
        val assembly2 = Assembly("wfos.red.detector", 5.seconds)
        par(
                { assembly1.submit(Setup("tcs.darknight", "command-1")) },
                { assembly2.submit(Setup("tcs.darknight", "command-1")) }
        )
    }
    // #onSetup


    // #onObserve
    onObserve("command2") {
        // do something
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
                val diagnosticEvent: SystemEvent = SystemEvent("esw.esw_darknight", "diagnostic")
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
        val triggerStartEvent = ObserveEvent("esw.command", "trigger.start", command(stringKey(name = "triggerTime")))
        // publishEvent fails with EventServerNotAvailable which fails onObserve handler
        // onGlobalError handler is called
        // Sequence is terminated with failure.
        publishEvent(triggerStartEvent)
    }

    // Scenario-2 onSetup handler fails - submit returns negative SubmitResponse
    onSetup("command-2") { command ->
        val assembly1 = Assembly("filter.wheel", 5.seconds)

        //Submit command to assembly return negative response. (error by default) onGlobalError handler is called.
        // Sequence is terminated with failure.
        assembly1.submit(command)
    }

    // Scenario-3
    onDiagnosticMode {startTime, hint ->
        //publishEvent fails with EventServerNotAvailable
        //onDiagnosticMode handler fails
        //onGlobalError is called. Sequence execution continues.
        publishEvent(ObserveEvent("esw.diagnostic.mode", hint))
    }

    onGlobalError { error ->
        val errorReason = stringKey("reason").set(error.reason)
        val observationEndEvent = ObserveEvent("esw.observation.end", "error", errorReason)
        publishEvent(observationEndEvent)
    }
    // #onGlobalError


}

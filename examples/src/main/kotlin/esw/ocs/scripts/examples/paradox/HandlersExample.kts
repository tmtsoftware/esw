@file:Suppress("UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox

import esw.ocs.dsl.core.script
import esw.ocs.dsl.par
import esw.ocs.dsl.params.invoke
import esw.ocs.dsl.params.set
import esw.ocs.dsl.params.stringKey
import kotlin.time.seconds

script {

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
        val assembly = Assembly("filter.wheel", 5.seconds)
        assembly.goOnline()
    }
    // #onGoOnline

    // #onGoOffline
    onGoOffline {
        // send command to downstream components
        val assembly = Assembly("filter.wheel", 5.seconds)
        assembly.goOffline()
    }
    // #onGoOffline

    // #onGlobalError
    // Scenario-1 handler fails
    onObserve("trigger-filter-wheel") { command ->
        val triggerStartEvent = ObserveEvent("esw.command", "trigger.start", command(stringKey(name = "triggerTime")))
        // publishEvent fails with EventServerNotAvailable which fails onObserve handler
        // onGlobalError handler is called
        publishEvent(triggerStartEvent)
    }

    // Scenario-2 submit returns negative SubmitResponse
    onSetup("command-2") { command ->
        val assembly = Assembly("filter.wheel", 5.seconds)

        //Submit commnad to assembly return negative response. (error by default) onGlobalError handler is called.
        assembly.submit(command)
    }

    onGlobalError {error ->
        val errorReason = stringKey("reason").set(error.reason)
        val observationEndEvent = ObserveEvent("esw.observation.end", "error", errorReason)
        publishEvent(observationEndEvent)
    }
    // #onGlobalError


}

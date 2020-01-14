@file:Suppress("UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox

import esw.ocs.dsl.core.script
import esw.ocs.dsl.par
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

}
/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.scripts.examples.epics

import csw.params.events.SystemEvent
import esw.ocs.dsl.core.FsmScript
import esw.ocs.dsl.params.add
import esw.ocs.dsl.params.intKey
import esw.ocs.dsl.params.params
import esw.ocs.dsl.params.stringKey

FsmScript("INIT") {
    // temperature Fsm states
    val OK = "OK"
    val WAITING = "WAITING"

    // main script Fsm states
    val INIT = "INIT"
    val STARTED = "STARTED"

    fun fsmEvent(value: Int) = SystemEvent("esw.FsmTestScript", "state", intKey("temperatureFsm").set(value))
    suspend fun publishState(baseEvent: SystemEvent, state: String) = publishEvent(baseEvent.add(stringKey(state).set(state)))

    val temperatureFsm = Fsm("TEMP", "OK") {
        state(OK) {
            become(WAITING, fsmEvent(20).params)
        }

        state(WAITING) { params ->
            val systemEvent = SystemEvent("esw.FsmTestScript", "WAITING").add(params)
            publishState(systemEvent, WAITING)
            completeFsm()
        }
    }

    state(INIT) {
        onSetup("command-1") { command ->
            temperatureFsm.start()
            temperatureFsm.await()
            become(STARTED, command.params)
        }
    }

    state(STARTED) { params ->
        onSetup("command-2") {
            val systemEvent = SystemEvent("esw.FsmTestScript", "STARTED").add(params)
            publishState(systemEvent, STARTED)
        }
    }
}

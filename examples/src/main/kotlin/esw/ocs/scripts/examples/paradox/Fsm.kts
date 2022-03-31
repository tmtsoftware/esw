/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox

import csw.params.core.generics.Key
import esw.ocs.dsl.core.script
import esw.ocs.dsl.epics.CommandFlag
import esw.ocs.dsl.epics.EventVariable
import esw.ocs.dsl.epics.Fsm
import esw.ocs.dsl.epics.ParamVariable
import esw.ocs.dsl.params.intKey
import esw.ocs.dsl.params.params
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.milliseconds

script {

    //#create-fsm
    val irisFsm: Fsm = Fsm(name = "iris-fsm", initState = "INIT") {
        // place to define all states of FSM
    }
    //#create-fsm

    //#start-fsm
    irisFsm.start()
    //#start-fsm

    //#await
    irisFsm.await()
    //#await

    //#subscribing
    // ------------ EventVariable ---------------
    //#event-var
    val eventVariable: EventVariable = EventVariable("ESW.IRIS_darkNight.temperature")
    //#subscribing

    eventVariable.getEvent() // to get the latest Event
    //#event-var


    //#subscribing
    eventVariable.bind(irisFsm)

    // ------------ ParamVariable ---------------
    //#polling
    val tempKey: Key<Int> = intKey("temperature")
    //#polling

    //#param-var
    val paramVariable: ParamVariable<Int> = ParamVariable(0, "ESW.temperature.temp", tempKey)
    //#subscribing

    paramVariable.getParam() // to get the current values of the parameter
    paramVariable.first() // to get the first value from the values of the parameter
    paramVariable.setParam(10, 11) // publishes the given values on event key

    paramVariable.getEvent() // to get the latest Event
    //#param-var

    val eventBasedVariable = paramVariable
    //#binding
    eventBasedVariable.bind(irisFsm)
    //#binding

    //#subscribing
    paramVariable.bind(irisFsm) // binds the FSM and event variable
    //#subscribing

    //#polling

    // ------------ ParamVariable ---------------
    val pollingParamVar: ParamVariable<Int> =
            ParamVariable(0, "ESW.temperature.temp", tempKey, 2.seconds)

    pollingParamVar.bind(irisFsm)

    // ------------ EventVariable ---------------
    val pollingEventVar = EventVariable("ESW.IRIS_darkNight.temperature", 2.seconds)
    pollingEventVar.bind(irisFsm)
    //#polling

    //#command-flag
    val flag = CommandFlag()
    flag.bind(irisFsm) // bind the FSM and command flag

    onSetup("setup-command") { command ->
        flag.set(command.params) // will set params and refreshes the bound FSMs with the new params
    }

    val params = flag.value() // extract the current params value in FSM
    //#command-flag

    val exampleFsm = Fsm(name = "example-fsm", initState = "INIT") {

        val condition = true

        //#define-state
        state("INIT") {
            // actions to be performed in this state
        }
        //#define-state

        state("BECOME-STATE") {

            //#entry
            entry {
                // do something
            }
            //#entry

            //#after
            after(100.milliseconds) {
                // do something
            }
            //#after

            //#state-transition
            become(state = "IN-PROGRESS")
            //#state-transition

            //#complete-fsm
            completeFsm()   // will complete the Fsm
            // anything after this will not be executed
            //#complete-fsm
        }

        val temparature = paramVariable

        //#state-transition-on-re-evaluation
        state("LOW") {
            //#on
            on(temparature.first() < 20) {
                // do something but state transition does not happen
            }

            on(temparature.first() >= 20) {
                // do something and transit state
                become("HIGH")
            }
            //#on
        }
        //#state-transition-on-re-evaluation
    }
}

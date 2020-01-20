@file:Suppress("UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox

import csw.params.core.generics.Key
import csw.params.core.models.Coords.Coord
import csw.params.core.models.JEqCoord
import esw.ocs.dsl.core.script
import esw.ocs.dsl.epics.CommandFlag
import esw.ocs.dsl.epics.EventVariable
import esw.ocs.dsl.epics.Fsm
import esw.ocs.dsl.params.Params
import esw.ocs.dsl.params.coordKey
import esw.ocs.dsl.params.intKey
import kotlin.time.milliseconds

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


    //#event-var
    //**  System Var **//
    val tempKey: Key<Int> = intKey("temperature")
    val systemVar: EventVariable<Int> = SystemVar(0, "esw.temperature.temp", tempKey)

    systemVar.bind(irisFsm) // binds the FSM and event variable

    //**  Observe Var **//
    val coordKey: Key<Coord> = coordKey("co-ordinates")
    val observeVar: EventVariable<Coord> = ObserveVar(JEqCoord.make(0, 0), "IRIS.observe.coord", coordKey)
    observeVar.get() // returns the value of the parameter from the latest event

    observeVar.bind(irisFsm) // binds the FSM and event variable

    observeVar.set(JEqCoord.make(1, 1)) // publishes the given value on event key
    //#event-var

    var params = Params(mutableSetOf())

    //#command-flag
    val flag = CommandFlag()
    flag.value() // way to extract the current params value

    flag.bind(irisFsm) // bind the FSM and command flag

    flag.set(params) // refreshes the bound FSMs with the new params
    //#command-flag

    val exampleFsm = Fsm(name = "example-fsm", initState = "INIT") {

        val condition = true

        //#define-state
        state("INIT") {
            // actions to be performed in this state
        }
        //#define-state

        state("BECOME-STATE") {

            entry {

            }

            //#on
            on(condition) {
                // executes this when condition is true
            }
            //#on

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
    }
}

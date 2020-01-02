package esw.ocs.scripts.examples.paradox

import esw.ocs.dsl.core.script

script {

    //#create-fsm
    val irisFsm = Fsm(name = "iris-fsm", initState = "INIT") {
        // place to define different states of FSM
    }
    //#create-fsm

    val exampleFsm = Fsm(name = "example-fsm", initState = "INIT") {
        //#define-state
        state("INIT") {
            // actions to be performed in this state
        }
        //#define-state

        state("BECOME-STATE") {
            //#state-transition
            become(state = "IN-PROGRESS")
            //#state-transition

            //#complete-fsm
            // will complete the Fsm
            completeFsm()
            // anything after this will not be executed
            //#complete-fsm
        }
    }
}

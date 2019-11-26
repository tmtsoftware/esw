package esw.ocs.scripts.examples.epics

import esw.ocs.dsl.core.FSMScript
import esw.ocs.dsl.params.booleanKey

FSMScript("INIT") {
    val triggerFlag = SystemVar(false, "tcs.triggerflag", booleanKey("flag"))

    val triggerFsm = FSM("triggerfsm", "Init") {
        state("Init") {
            println("Init state")
            become("Waiting")
        }

        state("WAITING") {
            on(triggerFlag.get()!!) {
                become("Done")
            }
        }

        state("DONE") {
            println("Done state")
            completeFSM()
        }
    }

    triggerFlag.bind(triggerFsm)

    //start FSM in background
    state("INIT") {
        onSetup("command-1") {
            println("${Thread.currentThread().name} — command1 received")

            triggerFsm.start()

            //Change triggerFlagKey which will trigger FSM
            val systemEvent = SystemEvent("tcs", "triggerflag", booleanKey("flag").set(true))
            println("********** ${systemEvent.eventName()}")
            publishEvent(systemEvent)

            //await on FSM to finish
            triggerFsm.await()
        }

        onSetup("command-2") {
            println("command2 received")
        }
    }

    onStop {
        //do some actions to stop

        //send stop command to downstream sequencer
        Sequencer("lgsf", "darknight").stop()
    }
}

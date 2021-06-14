package esw.ocs.scripts.examples.epics

import esw.ocs.api.models.ObsMode
import esw.ocs.dsl.core.FsmScript
import esw.ocs.dsl.highlevel.models.LGSF
import esw.ocs.dsl.params.booleanKey
import kotlin.time.Duration
import kotlin.time.seconds

FsmScript("INIT") {
    val triggerFlag = ParamVariable(false, "TCS.triggerflag", booleanKey("flag"))

    val triggerFsm = Fsm("triggerfsm", "Init") {
        state("Init") {
            println("Init state")
            become("Waiting")
        }

        state("WAITING") {
            on(triggerFlag.first()) {
                become("Done")
            }
        }

        state("DONE") {
            println("Done state")
            completeFsm()
        }
    }

    triggerFlag.bind(triggerFsm)

    //start Fsm in background
    state("INIT") {
        onSetup("command-1") {
            println("${Thread.currentThread().name} — command1 received")

            triggerFsm.start()

            //Change triggerFlagKey which will trigger Fsm
            val systemEvent = SystemEvent("TCS", "triggerflag", booleanKey("flag").set(true))
            println("********** ${systemEvent.eventName()}")
            publishEvent(systemEvent)

            //await on Fsm to finish
            triggerFsm.await()
        }

        onSetup("command-2") {
            println("command2 received")
        }
    }

    onStop {
        //do some actions to stop

        //send stop command to downstream sequencer
        Sequencer(LGSF, ObsMode("darknight"), Duration.seconds(10)).stop()
    }
}

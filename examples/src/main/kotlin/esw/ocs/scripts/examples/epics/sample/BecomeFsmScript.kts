/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.scripts.examples.epics.sample

import csw.params.core.generics.Parameter
import esw.ocs.dsl.core.FsmScript
import esw.ocs.dsl.highlevel.models.ESW
import esw.ocs.dsl.params.*
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

FsmScript("INIT") {

    val testAssembly = Assembly(ESW, "test", 10.seconds)
    val paramVariable = ParamVariable(true, "TCS.trigger", booleanKey("flag"))
    val flag = commandFlag()

    val fsm = Fsm("Trigger Fsm", "START") {
        state("START") {
            println("START state")
            delay(1000)
            val parameter: Parameter<Int> = intKey("encoder").set(1)
            val event = SystemEvent("TCS", "trigger.INIT.state", parameter)
            publishEvent(event)
            event.paramType()
            on(true) {
                become("READY", event.params)
            }
        }

        state("READY") { params ->
            val parameter: Parameter<Int> = params(intKey("encoder"))

            if (parameter.first == 1) {
                val k1 = intKey("trigger")
                become("WAITING", params.madd(k1.set(22)))
            }

            become("DONE")
        }

        state("WAITING") {
            println("WAITING state")
            become("DONE")
        }

        state("DONE") {
            publishEvent(SystemEvent("TCS", "trigger.DONE.state"))
            completeFsm()
        }
    }

    paramVariable.bind(fsm)
    flag.bind(fsm)

    state("INIT") {
        onSetup("command-1") { command ->
            testAssembly.submit(command)
            become("DATUMING", command.params.madd(intKey("encoder").set(30)))
        }

        onSetup("command-2") {
            fsm.start()
            fsm.await()
            become("FINISHED")
        }
    }

    state("DATUMING") { params ->
        onObserve("observe-command-1") { command ->
            flag.set(command.params)
            //do something
        }

        onObserve("observe-command-2") {
            if (params.exists(intKey("encoder"))) {
                become("FINISHED")
            }
        }
    }

    state("FINISHED") {
        // do something
    }

}

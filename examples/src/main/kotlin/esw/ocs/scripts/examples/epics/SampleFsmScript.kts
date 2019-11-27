package esw.ocs.scripts.examples.epics

import csw.params.core.generics.Parameter
import csw.params.javadsl.JKeyType
import esw.ocs.dsl.core.script
import esw.ocs.dsl.params.booleanKey
import esw.ocs.dsl.params.kMadd
import esw.ocs.dsl.params.set
import kotlinx.coroutines.delay

script {

    val processVar = SystemVar(true, "tcs.trigger", booleanKey("flag"))

    val fsm = FSM("Trigger FSM", "INIT") {
        state("INIT") {
            println("INIT state")
            delay(1000)
            val parameter: Parameter<Int> = JKeyType.IntKey().make("encoder").set(1)
            val event = SystemEvent("tcs", "trigger.INIT.state", parameter)
            publishEvent(event)
            on(true) {
                become("READY").with(event.jParamSet())
            }
        }

        state("READY") {
            val parameter: Parameter<Int> = JKeyType.IntKey().make("encoder").set(1)
            val event = SystemEvent("tcs", "trigger.READY.state")
            publishEvent(event)

            become("DONE").with(event.kMadd(parameter).jParamSet())
        }

        state("DONE") {
            publishEvent(SystemEvent("tcs", "trigger.DONE.state"))
            completeFSM()
        }
    }

    processVar.bind(fsm)

    onSetup("command-1") {
        fsm.start()
        fsm.await()
    }
}

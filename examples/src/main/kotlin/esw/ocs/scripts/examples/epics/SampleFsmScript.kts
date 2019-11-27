package esw.ocs.scripts.examples.epics

import csw.params.core.generics.Parameter
import csw.params.javadsl.JKeyType
import esw.ocs.dsl.core.script
import esw.ocs.dsl.params.booleanKey
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
                become("READY", event.jParamSet())
            }
        }

        state("READY") { params ->
            val parameter: Parameter<Int> = JKeyType.IntKey().make("encoder").set(1)
            become("DONE", params.plus(parameter))
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

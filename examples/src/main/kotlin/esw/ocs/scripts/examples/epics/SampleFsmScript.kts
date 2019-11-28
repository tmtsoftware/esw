package esw.ocs.scripts.examples.epics

import csw.params.core.generics.Parameter
import esw.ocs.dsl.core.script
import esw.ocs.dsl.params.*
import kotlinx.coroutines.delay

script {

    val processVar = SystemVar(true, "tcs.trigger", booleanKey("flag"))
    val flag = commandFlag()

    val fsm = FSM("Trigger FSM", "INIT") {
        state("INIT") {
            println("INIT state")
            delay(1000)
            val parameter: Parameter<Int> = intKey("encoder").set(1)
            val event = SystemEvent("tcs", "trigger.INIT.state", parameter)
            publishEvent(event)
            event.paramType()
            on(true) {
                become("READY", Params(event.jParamSet()))
            }
        }

        state("READY") { params ->
            val parameter: Parameter<Int> = params(intKey("encoder"))

            if(parameter.first == 1) {
                val k1 = intKey("trigger")
                become("WAITING", params.kMadd(k1.set(22)))
            }

            onSetup("command-1") { command ->
                println("command-1 received")
                flag.set(command.jParamSet())
            }

            become("DONE")
        }

        state("WAITING") {
            println("WAITING state")
            become("DONE")
        }

        state("DONE") {
            publishEvent(SystemEvent("tcs", "trigger.DONE.state"))
            completeFSM()
        }
    }

    processVar.bind(fsm)
    flag.bind(fsm)

    onSetup("command-1") {
        fsm.start()
        fsm.await()
    }
}

package esw.ocs.scripts.examples.epics.sample

import csw.params.core.generics.Parameter
import esw.ocs.dsl.core.FSMScript
import esw.ocs.dsl.params.*
import kotlinx.coroutines.delay

FSMScript("INIT") {

    val testAssembly = Assembly("test")
    val processVar = SystemVar(true, "tcs.trigger", booleanKey("flag"))
    val flag = commandFlag()

    val fsm = FSM("Trigger FSM", "START") {
        state("START") {
            println("START state")
            delay(1000)
            val parameter: Parameter<Int> = intKey("encoder").set(1)
            val event = SystemEvent("tcs", "trigger.INIT.state", parameter)
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
                become("WAITING", params.kMadd(k1.set(22)))
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

    state("INIT") {
        onSetup("command-1") { command ->
            testAssembly.submit(command)
            become("DATUMING", command.params.kMadd(intKey("encoder").set(30)))
        }

        onSetup("command-2") {
            fsm.start()
            fsm.await()
            become("FINISHED")
        }
    }

    state("DATUMING") { params ->
        onObserve("observe-command-1") {
            //do something
        }

        onObserve("observe-command-2") {
            if (params.kExists(intKey("encoder"))) {
                become("FINISHED")
            }
        }
    }

    state("FINISHED") {
        // do something
    }

}

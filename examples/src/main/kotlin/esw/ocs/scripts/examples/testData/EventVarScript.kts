package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.script
import kotlin.time.Duration

script {

    val eventVar = EventVariable("TCS.polling.event-var-test", Duration.milliseconds(400))
    val eventVarFsm = Fsm("event-pollingTest", "INIT") {
        state("INIT") {
            val event = SystemEvent("TCS.polling", "event-var-test")
            publishEvent(event)
        }
    }

    eventVar.bind(eventVarFsm)

    onSetup("start-event-fsm") {
        eventVarFsm.start()
    }

}
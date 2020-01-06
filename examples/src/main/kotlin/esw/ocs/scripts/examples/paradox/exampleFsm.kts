package esw.ocs.scripts.examples.paradox

import csw.params.events.SystemEvent
import esw.ocs.dsl.core.script
import esw.ocs.dsl.params.longKey
import esw.ocs.dsl.params.stringKey

script {
    //#example-fsm
    val tempKey = longKey("temperature")
    val stateKey = stringKey("state")

    val tempFsmEvent = SystemEvent("esw.temperatureFsm", "state")
    suspend fun publishState(baseEvent: SystemEvent, state: String) = publishEvent(baseEvent.add(stateKey.set(state)))

    // temperature Fsm states
    val OK = "OK"
    val ERROR = "ERROR"
    val FINISHED = "FINISHED"

    val temperatureVar = SystemVar(0, "esw.temperature.temp", tempKey)

    val temperatureFsm = Fsm("TEMP", OK) {
        var fsmVariable = 10                    // [[ 01 ]]

        state(OK) {
            // [[ 02 ]]

            entry {
                publishState(tempFsmEvent, OK)
            }
            on(temperatureVar.get() == 30L) {
                become(FINISHED)             // [[ 03 ]]
            }
            on(temperatureVar.get() > 40) {
                become(ERROR)
            }
        }

        state(ERROR) {
            entry {
                publishState(tempFsmEvent, ERROR)
            }
            on(temperatureVar.get() < 40) {
                become(OK)
            }
        }

        state(FINISHED) {
            completeFsm()                   // [[ 04 ]]
        }
    }

    temperatureVar.bind(temperatureFsm)     // [[ 05 ]]

    onSetup("command-1") {
        temperatureFsm.start()              // [[ 06 ]]
    }

    onSetup("command-2") {
        temperatureFsm.await()              // [[ 07 ]]
    }

    //#example-fsm
}

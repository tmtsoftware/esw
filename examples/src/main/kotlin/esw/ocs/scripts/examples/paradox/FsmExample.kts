@file:Suppress("UNUSED_VARIABLE")

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
        var fsmVariable = 10                    // [[ 1 ]]

        state(OK) {
            // [[ 2 ]]

            entry {
                publishState(tempFsmEvent, OK)
            }
            on(temperatureVar.get() == 30L) {
                become(FINISHED)             // [[ 3 ]]
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
            completeFsm()                   // [[ 4 ]]
        }
    }

    temperatureVar.bind(temperatureFsm)     // [[ 5 ]]

    onSetup("command-1") {
        temperatureFsm.start()              // [[ 6 ]]
    }

    onSetup("command-2") {
        temperatureFsm.await()              // [[ 7 ]]
    }

    //#example-fsm
}

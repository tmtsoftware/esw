@file:Suppress("UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox

import csw.params.events.SystemEvent
import esw.ocs.dsl.core.script
import esw.ocs.dsl.epics.CommandFlag
import esw.ocs.dsl.params.*

script {
    //#example-fsm
    val tempKey = longKey("temperature")
    val stateKey = stringKey("state")

    val tempFsmEvent = SystemEvent("esw.temperatureFsm", "state")
    suspend fun publishState(baseEvent: SystemEvent, state: String) =
            publishEvent(baseEvent.add(stateKey.set(state)))

    // temperature Fsm states
    val OK = "OK"
    val ERROR = "ERROR"
    val FINISHED = "FINISHED"

    val temperatureVar = SystemVar(0, "esw.temperature.temp", tempKey)
    val commandFlag = CommandFlag()

    val temperatureFsm = Fsm("TEMP", OK) {
        var fsmVariable = 10                                     // [[ 1 ]]

        state(OK) {
            val currentTemp = temperatureVar.get()        // [[ 2 ]]
            val expectedTemp = commandFlag.value().get(intKey("expected-temperature")).get().first

            entry {
                publishState(tempFsmEvent, OK)
            }
            on(currentTemp == 30L) {
                become(FINISHED)                                 // [[ 3 ]]
            }
            on(currentTemp > 40) {
                become(ERROR)
            }
            on(currentTemp <= expectedTemp) {
                info("temperataure is below expected threshold",
                        mapOf("exepected" to expectedTemp, "current" to currentTemp)
                )
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
            completeFsm()                                        // [[ 4 ]]
        }
    }

    temperatureVar.bind(temperatureFsm)
    commandFlag.bind(temperatureFsm)                             // [[ 5 ]]

    onSetup("command-1") {
        temperatureFsm.start()                                   // [[ 6 ]]
    }

    onSetup("command-2") { command ->
        commandFlag.set(command.params)                          // [[ 7 ]]
    }

    onSetup("command-3") {
        temperatureFsm.await()                                   // [[ 8 ]]
    }

    //#example-fsm
}

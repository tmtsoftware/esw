@file:Suppress("UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox

import csw.params.events.SystemEvent
import esw.ocs.dsl.core.script
import esw.ocs.dsl.epics.CommandFlag
import esw.ocs.dsl.params.*

script {
    //#example-fsm

    // method to publish the state of the FSM
    val stateKey = stringKey("state")
    val tempFsmEvent = SystemEvent("esw.temperatureFsm", "state")
    suspend fun publishState(baseEvent: SystemEvent, state: String) =
            publishEvent(baseEvent.add(stateKey.set(state)))

    // temperature Fsm states
    val OK = "OK"
    val ERROR = "ERROR"
    val FINISHED = "FINISHED"

    // Event-based variable for current temperature
    val tempKey = longKey("temperature")
    val temperatureVar = ParamVariable(0, "esw.temperature.temp", tempKey)

    // CommandFlag, and method to get expected temperature from it
    val commandFlag = CommandFlag()
    fun getTemperatureLimit(defaultTemperatureLimit: Int): Int {
        val tempLimitParameter = commandFlag.value().get(intKey("temperatureLimit"))
        return if (tempLimitParameter.isDefined)
            tempLimitParameter.get().first
        else
            defaultTemperatureLimit
    }

    // key for parameter passed to Error state from Ok state
    val deltaKey = longKey("delta")

    // FSM definition
    val temperatureFsm = Fsm("TEMP", OK) {
        val initialTemperatureLimit = 40                         // [[ 1 ]]

        state(OK) {
            val currentTemp = temperatureVar.first()             // [[ 2 ]]
            val tempLimit = getTemperatureLimit(initialTemperatureLimit)

            entry {
                publishState(tempFsmEvent, OK)                   // [[ 3 ]]
            }
            on(currentTemp == 30L) {
                become(FINISHED)                                 // [[ 4 ]]
            }
            on(currentTemp > tempLimit) {
                val deltaParam = deltaKey.set(currentTemp - tempLimit)
                become(ERROR, Params(setOf(deltaParam)))         // [[ 5 ]]
            }
            on(currentTemp <= tempLimit) {
                info("temperature is below expected threshold",
                        mapOf("limit" to tempLimit, "current" to currentTemp)
                )
            }
        }

        state(ERROR) { params ->
            val tempLimit = getTemperatureLimit(initialTemperatureLimit)

            entry {
                info("temperature is above expected threshold",
                        mapOf("limit" to tempLimit, "delta" to params(deltaKey).first)
                )
                publishState(tempFsmEvent, ERROR)
            }
            on(temperatureVar.first() < tempLimit) {
                become(OK)
            }
        }

        state(FINISHED) {
            completeFsm()                                        // [[ 6 ]]
        }
    }

    // bind reactives to FSM
    temperatureVar.bind(temperatureFsm)
    commandFlag.bind(temperatureFsm)                             // [[ 7 ]]

    // Command handlers
    onSetup("startFSM") {
        temperatureFsm.start()                                   // [[ 8 ]]
    }

    onSetup("changeTemperatureLimit") { command ->
        commandFlag.set(command.params)                          // [[ 9 ]]
    }

    onSetup("waitForFSM") {
        temperatureFsm.await()                                   // [[ 10 ]]
        info("FSM is no longer running.")
    }

    //#example-fsm

}

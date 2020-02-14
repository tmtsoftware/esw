package esw.ocs.scripts.examples.epics

import csw.params.events.SystemEvent
import esw.ocs.dsl.core.FsmScript
import esw.ocs.dsl.params.first
import esw.ocs.dsl.params.invoke
import esw.ocs.dsl.params.longKey
import esw.ocs.dsl.params.stringKey
import kotlinx.coroutines.delay

FsmScript("INIT") {
    // temperature Fsm states
    val OK = "OK"
    val ERROR = "ERROR"

    // main script Fsm states
    val INIT = "INIT"
    val STARTED = "STARTED"
    val TERMINATE = "TERMINATE"

    val commandFsmEvent = SystemEvent("esw.commandFsm", "state")
    val tempFsmEvent = SystemEvent("esw.temperatureFsm", "state")

    val tempKey = longKey("temperature")
    val stateKey = stringKey("state")

    val temperatureVar = SystemVar(0, "esw.temperature.temp", tempKey)

    suspend fun publishState(baseEvent: SystemEvent, state: String) = publishEvent(baseEvent.add(stateKey.set(state)))

    /**
     * temp == 30               => FINISH
     * temp > 40 or temp < 20   => ERROR
     * else                     => OK
     */
    val temperatureFsm = Fsm("TEMP", OK) {
        state(OK) {
            entry {
                publishState(tempFsmEvent, OK)
            }

            on(temperatureVar.first() == 30L) {
                completeFsm()
            }

            on(temperatureVar.first() > 40) {
                become(ERROR)
            }
        }

        state(ERROR) {
            entry {
                publishState(tempFsmEvent, ERROR)
            }

            on(temperatureVar.first() < 40) {
                become("OK")
            }
        }
    }

    temperatureVar.bind(temperatureFsm)

    /**
     * 1. INIT =>
     *      1.1 start Temperature Fsm
     * 2. STARTED  =>
     *      2.1 receive cmd and set temperature process var
     *      2.2 if temp > 50 then TERMINATE
     *      2.3 else goto 2.1
     * 3. TERMINATE    =>
     *      3.1 shutdown
     * 4. shutdown
     */
    state(INIT) {
        publishState(commandFsmEvent, INIT)
        temperatureFsm.start()
        become(STARTED)
    }

    state(STARTED) {
        publishState(commandFsmEvent, STARTED)

        onSetup("set-temp") { cmd ->
            val receivedTemp = cmd(tempKey).first
            publishEvent(SystemEvent("esw.temperature", "temp", tempKey.set(receivedTemp)))

            if (receivedTemp == 30L) {
                temperatureFsm.await()
                publishState(tempFsmEvent, "FINISHED")
            }

            if (receivedTemp > 50L) {
                become("TERMINATE")
            }
        }
    }

    state(TERMINATE) {
        publishState(commandFsmEvent, TERMINATE)

        onObserve("wait") {
            delay(10000)
        }

        onStop {
            publishState(commandFsmEvent, "Fsm:TERMINATE:STOP")
        }
    }

    onStop {
        publishState(commandFsmEvent, "MAIN:STOP")
    }
}

package esw.ocs.scripts.examples.epics


import csw.params.events.SystemEvent
import esw.ocs.dsl.core.FSMScript
import esw.ocs.dsl.params.first
import esw.ocs.dsl.params.invoke
import esw.ocs.dsl.params.longKey
import esw.ocs.dsl.params.stringKey
import kotlinx.coroutines.delay

FSMScript("INIT") {
    // temperature FSM states
    val OK = "OK"
    val ERROR = "ERROR"

    // main script FSM states
    val INIT = "INIT"
    val STARTED = "STARTED"
    val TERMINATE = "TERMINATE"

    val commandFSMEvent = SystemEvent("esw.commandFSM", "state")
    val tempFSMEvent = SystemEvent("esw.temperatureFSM", "state")

    val tempKey = longKey("temperature")
    val stateKey = stringKey("state")

    val temperatureVar = SystemVar(0, "esw.temperature.temp", tempKey)

    suspend fun publishSate(baseEvent: SystemEvent, state: String) = publishEvent(baseEvent.add(stateKey.set(state)))

    /**
     * temp == 30               => FINISH
     * temp > 40 or temp < 20   => ERROR
     * else                     => OK
     */
    val temperatureFSM = FSM("TEMP", "OK") {
        state(OK) {
            entry {
                publishSate(tempFSMEvent, OK)
            }

            on(temperatureVar.get() == 30L) {
                completeFSM()
            }

            on(temperatureVar.get() > 40) {
                become(ERROR)
            }
        }

        state(ERROR) {
            entry {
                publishSate(tempFSMEvent, ERROR)
            }

            on(temperatureVar.get() < 40) {
                become("OK")
            }
        }
    }

    temperatureVar.bind(temperatureFSM)

    /**
     * 1. INIT =>
     *      1.1 start Temperature FSM
     * 2. STARTED  =>
     *      2.1 receive cmd and set temperature process var
     *      2.2 if temp > 50 then TERMINATE
     *      2.3 else goto 2.1
     * 3. TERMINATE    =>
     *      3.1 shutdown
     * 4. shutdown
     */
    state(INIT) {
        publishSate(commandFSMEvent, INIT)
        temperatureFSM.start()
        become(STARTED)
    }

    state(STARTED) {
        publishSate(commandFSMEvent, STARTED)

        onSetup("set-temp") { cmd ->
            val receivedTemp = cmd(tempKey).first
            publishEvent(SystemEvent("esw.temperature", "temp", tempKey.set(receivedTemp)))

            if (receivedTemp == 30L) {
                temperatureFSM.await()
                publishSate(tempFSMEvent, "FINISHED")
            }

            if (receivedTemp > 50L) {
                become("TERMINATE")
            }
        }
    }

    state(TERMINATE) {
        publishSate(commandFSMEvent, TERMINATE)

        onObserve("wait") {
            delay(10000)
        }

        onStop {
            publishSate(commandFSMEvent, "FSM:TERMINATE:STOP")
        }
    }

    onStop {
        publishSate(commandFSMEvent, "MAIN:STOP")
    }
}
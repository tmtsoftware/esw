package esw.ocs.scripts.examples.epics

import csw.params.events.SystemEvent
import esw.ocs.dsl.core.FsmScript
import esw.ocs.dsl.epics.CommandFlag
import esw.ocs.dsl.params.add
import esw.ocs.dsl.params.intKey
import esw.ocs.dsl.params.params
import esw.ocs.dsl.params.stringKey

FsmScript("INIT") {
    // temperature Fsm states
    val OK = "OK"
    val WAITING = "WAITING"

    // main script Fsm states
    val INIT = "INIT"

    // command flag
    val commandFlag = CommandFlag()

    suspend fun publishState(baseEvent: SystemEvent, state: String) = publishEvent(baseEvent.add(stringKey(state).set(state)))

    val temperatureFsm = Fsm("TEMP", "OK") {
        state(OK) {
            become(WAITING)
        }

        state(WAITING) {
            // check command flag for observe param key
            on(commandFlag.value().exists(intKey("observe"))) {
                val systemEvent = SystemEvent("esw.CommandFlagFsmTestScript", "OBSERVE").add(commandFlag.value())
                publishState(systemEvent, WAITING)
                completeFsm()
            }
        }
    }

    commandFlag.bind(temperatureFsm)

    state(INIT) {
        onObserve("observe-command-1") {
            temperatureFsm.start()
        }

        onObserve("observe-command-2") { command ->
            // extract observe command params and set commandFlag
            // setting command flag will refresh temperature fsm
            commandFlag.set(command.params)
        }
    }
}

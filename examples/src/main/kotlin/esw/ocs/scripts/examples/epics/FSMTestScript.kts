package esw.ocs.scripts.examples.epics


import csw.params.core.models.Prefix
import csw.params.events.EventName
import csw.params.events.SystemEvent
import esw.ocs.dsl.core.FSMScript
import esw.ocs.dsl.params.Params
import esw.ocs.dsl.params.intKey
import esw.ocs.dsl.params.set
import esw.ocs.dsl.params.stringKey

FSMScript("INIT") {
    // temperature FSM states
    val OK = "OK"
    val WAITING = "WAITING"

    // main script FSM states
    val INIT = "INIT"
    val STARTED = "STARTED"

    fun fsmEvent(value: Int) = SystemEvent("esw.FSMTestScript", "state", intKey("temperatureFSM").set(value))
    suspend fun publishState(baseEvent: SystemEvent, state: String) = publishEvent(baseEvent.add(stringKey(state).set(state)))

    val temperatureFSM = FSM("TEMP", "OK") {
        state(OK) {
            become(WAITING, Params(fsmEvent(20).jParamSet()))
        }

        state(WAITING) { params ->
            val systemEvent = SystemEvent(Prefix("esw.FSMTestScript"), EventName("WAITING")).jMadd(params.jParamSet())
            publishState(systemEvent, WAITING)
            completeFSM()
        }
    }

    state(INIT) {
        onSetup("command-1") { command ->
            temperatureFSM.start()
            temperatureFSM.await()
            become(STARTED, Params(command.jParamSet()))
        }

    }

    state(STARTED) { params ->
        onSetup("command-2") { _ ->
            val systemEvent = SystemEvent(Prefix("esw.FSMTestScript"), EventName("STARTED")).jMadd(params.jParamSet())
            publishState(systemEvent, STARTED)
        }
    }
}

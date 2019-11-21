package esw.ocs.dsl.highlevel

import csw.params.core.generics.Key
import esw.ocs.dsl.epics.ProcessVariable
import esw.ocs.dsl.epics.StateMachine
import esw.ocs.dsl.params.set
import kotlinx.coroutines.CoroutineScope

interface FSMDsl {
    val coroutineScope: CoroutineScope

    suspend fun FSM(name: String, initState: String, block: suspend StateMachine.() -> Unit) =
            StateMachine(name, initState, coroutineScope).apply { block() }

    suspend fun <T> EventServiceDsl.SystemVar(initial: T, eventKeyStr: String, key: Key<T>): ProcessVariable<T> {
        val eventKey = EventKey(eventKeyStr)
        val systemEvent = SystemEvent(eventKey.source().prefix(), eventKey.eventName().name(), key.set(initial))
        return ProcessVariable(systemEvent, key, this)
    }

    suspend fun <T> EventServiceDsl.ObserveVar(initial: T, eventKeyStr: String, key: Key<T>): ProcessVariable<T> {
        val eventKey = EventKey(eventKeyStr)
        val observeEvent = ObserveEvent(eventKey.source().prefix(), eventKey.eventName().name(), key.set(initial))
        return ProcessVariable(observeEvent, key, this)
    }


}

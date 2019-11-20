package esw.ocs.dsl.highlevel

import csw.params.core.generics.Key
import esw.ocs.dsl.epics.ProcessVariable
import esw.ocs.dsl.epics.StateMachine
import kotlinx.coroutines.CoroutineScope

interface FSMDsl {
    val coroutineScope: CoroutineScope

    suspend fun FSM(name: String, initState: String, block: suspend StateMachine.() -> Unit) =
        StateMachine(name, initState, coroutineScope).apply { block() }

    suspend fun <T> EventServiceDsl.ProcessVar(initial: T, eventKey: String, key: Key<T>): ProcessVariable<T> =
        ProcessVariable<T>(initial, eventKey, key, this)
}

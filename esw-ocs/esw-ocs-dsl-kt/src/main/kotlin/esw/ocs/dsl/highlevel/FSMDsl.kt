package esw.ocs.dsl.highlevel

import esw.ocs.dsl.epics.StateMachine
import kotlinx.coroutines.CoroutineScope

interface FSMDsl {
    val coroutineScope : CoroutineScope

    suspend fun FSM(name:String, block:suspend StateMachine.()-> Unit) =
            StateMachine(name,coroutineScope).apply { block() }

}

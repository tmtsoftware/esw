package esw.ocs.dsl.epics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.LAZY
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration

interface FSMState {
    fun become(state: String)
    fun completeFsm()
    suspend fun on(condition: Boolean = true, body: suspend () -> Unit)
    suspend fun on(duration: Duration, body: suspend () -> Unit)
    suspend fun entry(body: suspend () -> Unit)
    fun state(name: String, block: suspend () -> Unit)
}

interface StateMachine : Refreshable {
    fun start()
    suspend fun await()
}

class StateMachineImpl(private val name: String, initialState: String, val coroutineScope: CoroutineScope) : StateMachine, FSMState {
    // fixme: Try and remove optional behavior of both variables
    private var currentState: String? = null
    private var previousState: String? = null

    //fixme : do we need to pass as receiver coroutine scope to state lambda
    private val states = mutableMapOf<String, suspend () -> Unit>()

    private var fsmJob: Job = coroutineScope.launch(start = LAZY) {
        become(initialState)
    }

    override fun state(name: String, block: suspend () -> Unit) {
        states += name to block
    }

    override fun become(state: String) {
        if (states.keys.any { it.equals(state, true) }) {
            previousState = currentState
            currentState = state
            refresh()
        } else throw InvalidStateException(state)
    }

    override fun start() {
        fsmJob.start()
    }

    override suspend fun await() {
        fsmJob.join()
    }

    override fun completeFsm() {
        fsmJob.cancel()
    }

    override fun refresh() {
        coroutineScope.launch(fsmJob) { states[currentState]?.invoke() }
    }

    override suspend fun on(condition: Boolean, body: suspend () -> Unit) {
        if (condition) {
            body()
        }
    }

    override suspend fun on(duration: Duration, body: suspend () -> Unit) {
        delay(duration.toLongMilliseconds())
        on(body = body)
    }

    override suspend fun entry(body: suspend () -> Unit) {
        if (currentState != previousState) {
            body()
        }
    }

    // todo: can we use generics here?
    operator fun Int?.compareTo(other: Int?): Int =
            if (this != null && other != null) this.compareTo(other)
            else -1
}

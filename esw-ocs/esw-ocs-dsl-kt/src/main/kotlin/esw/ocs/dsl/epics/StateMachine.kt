package esw.ocs.dsl.epics

import kotlinx.coroutines.*
import kotlin.time.Duration

interface FSMState {
    fun become(state: String)
    fun completeFsm()
    suspend fun on(condition: Boolean = true, body: suspend () -> Unit)
    suspend fun after(duration: Duration, body: suspend () -> Unit)
    suspend fun entry(body: suspend () -> Unit)
    fun state(name: String, block: suspend () -> Unit)
}

interface StateMachine : Refreshable {
    fun start()
    suspend fun await()
}

// Don't remove name parameter, it will used while logging.
class StateMachineImpl(val name: String, val initialState: String, val coroutineScope: CoroutineScope) : StateMachine, FSMState {
    // fixme: Try and remove optional behavior of both variables
    private var currentState: String? = null
    private var previousState: String? = null

    //fixme : do we need to pass as receiver coroutine scope to state lambda
    private val states = mutableMapOf<String, suspend () -> Unit>()

    //this is done to make new job child of the coroutine scope's job.
    private val fsmJob: CompletableJob = Job(coroutineScope.coroutineContext[Job])

    override fun state(name: String, block: suspend () -> Unit) {
        states += name.toUpperCase() to block
    }

    override fun become(state: String) {
        if (states.keys.any { it.equals(state, true) }) {
            previousState = currentState
            currentState = state
            refresh()
        } else throw InvalidStateException(state)
    }

    override fun start() {
        become(initialState)
    }

    override suspend fun await() {
        fsmJob.join()
    }

    override fun completeFsm() {
        fsmJob.cancel()
    }

    override fun refresh() {
        coroutineScope.launch(fsmJob) {
            states[currentState?.toUpperCase()]?.invoke()
        }
    }

    override suspend fun on(condition: Boolean, body: suspend () -> Unit) {
        if (condition) {
            body()
        }
    }

    override suspend fun after(duration: Duration, body: suspend () -> Unit) {
        delay(duration.toLongMilliseconds())
        on(body = body)
    }

    override suspend fun entry(body: suspend () -> Unit) {
        if (currentState.equals(previousState, true)) {
            body()
        }
    }

    // todo: can we use generics here?
    operator fun Int?.compareTo(other: Int?): Int =
            if (this != null && other != null) this.compareTo(other)
            else -1
}

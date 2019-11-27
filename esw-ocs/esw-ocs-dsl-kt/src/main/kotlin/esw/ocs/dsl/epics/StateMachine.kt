package esw.ocs.dsl.epics

import esw.ocs.dsl.params.Params
import kotlinx.coroutines.*
import kotlin.time.Duration

@DslMarker
annotation class FSMDslMarker

// this interface is exposed to outside world
interface StateMachine : Refreshable {
    fun start()
    suspend fun await()
}

// this interface is exposed at top level of FSM
@FSMDslMarker
interface FSMTopLevel {
    fun state(name: String, block: suspend FSMState.(params: Params) -> Unit)
}

// this interface is exposed in side each state of FSM
@FSMDslMarker
interface FSMState {
    fun become(state: String, params: Params = Params(setOf()))
    fun completeFSM()
    suspend fun on(condition: Boolean = true, body: suspend () -> Unit)
    suspend fun after(duration: Duration, body: suspend () -> Unit)
    suspend fun entry(body: suspend () -> Unit)
}

// Don't remove name parameter, it will used while logging.
class StateMachineImpl(val name: String, val initialState: String, val coroutineScope: CoroutineScope) : StateMachine, FSMTopLevel, FSMState {

    // fixme: Try and remove optional behavior of both variables
    private var currentState: String? = null
    private var previousState: String? = null
    private var params: Params = Params(setOf())

    //fixme : do we need to pass as receiver coroutine scope to state lambda
    private val states = mutableMapOf<String, suspend FSMState.(params: Params) -> Unit>()

    //this is done to make new job child of the coroutine scope's job.
    private val fsmJob: CompletableJob = Job(coroutineScope.coroutineContext[Job])

    override fun state(name: String, block: suspend FSMState.(params: Params) -> Unit) {
        states += name.toUpperCase() to block
    }

    override fun become(state: String, params: Params) {
        if (states.keys.any { it.equals(state, true) }) {
            previousState = currentState
            currentState = state
            this.params = params
            refresh()
        } else throw InvalidStateException(state)
    }

    override fun start() {
        become(initialState)
    }

    override suspend fun await() {
        fsmJob.join()
    }

    override fun completeFSM() {
        fsmJob.cancel()
    }

    override fun refresh() {
        coroutineScope.launch(fsmJob) {
            states[currentState?.toUpperCase()]?.invoke(this@StateMachineImpl, params)
        }
    }

    override suspend fun on(condition: Boolean, body: suspend () -> Unit) {
        if (condition) {
            body()
        }
    }

    override suspend fun after(duration: Duration, body: suspend () -> Unit) {
        delay(duration.toLongMilliseconds())
        body()
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

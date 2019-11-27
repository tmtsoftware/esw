package esw.ocs.dsl.epics

import csw.params.core.generics.Parameter
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
    fun state(name: String, block: suspend FSMState.() -> Unit)
}

interface ParameterUtil {
    fun with(params: MutableSet<Parameter<*>>?)
}

// this interface is exposed in side each state of FSM
@FSMDslMarker
interface FSMState {
    var params: MutableSet<Parameter<*>>?
    fun become(state: String): ParameterUtil
    fun completeFSM()
    suspend fun on(condition: Boolean = true, body: suspend () -> Unit)
    suspend fun after(duration: Duration, body: suspend () -> Unit)
    suspend fun entry(body: suspend () -> Unit)
}

// Don't remove name parameter, it will used while logging.
class StateMachineImpl(val name: String, val initialState: String, val coroutineScope: CoroutineScope) : StateMachine, FSMTopLevel, FSMState, ParameterUtil {

    // fixme: Try and remove optional behavior of both variables
    private var currentState: String? = null
    private var previousState: String? = null

    override var params: MutableSet<Parameter<*>>? = null

    //fixme : do we need to pass as receiver coroutine scope to state lambda
    private val states = mutableMapOf<String, suspend FSMState.() -> Unit>()

    //this is done to make new job child of the coroutine scope's job.
    private val fsmJob: CompletableJob = Job(coroutineScope.coroutineContext[Job])

    override fun state(name: String, block: suspend FSMState.() -> Unit) {
        states += name.toUpperCase() to block
    }

    override fun become(state: String): ParameterUtil {
        if (states.keys.any { it.equals(state, true) }) {
            previousState = currentState
            currentState = state
            refresh()
            return this
        } else throw InvalidStateException(state)
    }

    override fun with(params: MutableSet<Parameter<*>>?) {
        this.params = params
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
            states[currentState?.toUpperCase()]?.invoke(this@StateMachineImpl)
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

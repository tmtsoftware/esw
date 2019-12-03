package esw.ocs.dsl.epics

import esw.ocs.dsl.FSMMarker
import esw.ocs.dsl.params.Params
import kotlinx.coroutines.*
import kotlin.time.Duration

// this interface is exposed to outside world
interface StateMachine : Refreshable {
    suspend fun start()
    suspend fun await()
}

// this interface is exposed at top-level of FSM
@FSMMarker
interface FSMScope {
    fun state(name: String, block: suspend FSMStateScope.(params: Params) -> Unit)
}

// this interface is exposed in side each state of FSM
@FSMMarker
interface FSMStateScope {
    suspend fun become(state: String, params: Params = Params(setOf()))
    suspend fun completeFSM()
    suspend fun on(condition: Boolean = true, body: suspend () -> Unit)
    suspend fun after(duration: Duration, body: suspend () -> Unit)
    suspend fun entry(body: suspend () -> Unit)
}

// Don't remove name parameter, it will be used while logging.
class StateMachineImpl(val name: String, private val initialState: String, val coroutineScope: CoroutineScope) : StateMachine, FSMScope, FSMStateScope {
    // fixme: Try to remove optional behavior of both variables
    private var currentState: String? = null
    private var previousState: String? = null
    private var params: Params = Params(setOf())
    private var fsmSubscriptions: Set<FSMSubscription> = setOf()


    //fixme : do we need to pass as receiver coroutine scope to state lambda
    private val states = mutableMapOf<String, suspend FSMStateScope.(params: Params) -> Unit>()

    //this is done to make new job child of the coroutine scope's job.
    private val fsmJob: CompletableJob = Job(coroutineScope.coroutineContext[Job])

    override fun state(name: String, block: suspend FSMStateScope.(params: Params) -> Unit) {
        states += name.toUpperCase() to block
    }

    override suspend fun become(state: String, params: Params) {
        if (states.keys.any { it.equals(state, true) }) {
            previousState = currentState
            currentState = state
            this.params = params
            coroutineScope.launch(fsmJob) {
                states[currentState?.toUpperCase()]?.invoke(this@StateMachineImpl, params)
            }.join()
        } else throw InvalidStateException(state)
    }

    override suspend fun start() = become(initialState)

    override suspend fun await() = fsmJob.join()

    override suspend fun completeFSM() {
        fsmJob.cancel()
        fsmSubscriptions.forEach { it.cancel() }
    }

    override suspend fun refresh() {
        currentState?.let { become(it, params) }
    }

    override fun addFSMSubscription(fsmSubscription: FSMSubscription) {
        fsmSubscriptions = fsmSubscriptions + fsmSubscription
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
        if (!currentState.equals(previousState, true)) {
            body()
        }
    }
}

package esw.ocs.dsl.epics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.LAZY
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration

class StateMachine(private val name: String, initialState: String, val coroutineScope: CoroutineScope) : Refreshable {
    // fixme: Try and remove optional behavior of both variables
    private var currentState: String? = null
    private var previousState: String? = null

    //fixme : do we need to pass as receiver coroutine scope to state lambda
    private val states = mutableMapOf<String, suspend () -> Unit>()

    private var fsmJob: Job = coroutineScope.launch(start = LAZY) {
        become(initialState)
    }

    fun state(name: String, block: suspend () -> Unit) {
        states += name to block
    }

    suspend fun become(state: String) {
        if (states.keys.any { it.equals(state, true) }) {
            previousState = currentState
            currentState = state
            refresh()
            //fixme: add concerete exception for this
        } else throw RuntimeException("Failed transition to invalid state:  $state")
    }

    fun start() {
        fsmJob.start()
    }

    suspend fun await() {
        fsmJob.join()
    }

    fun completeFsm() {
        fsmJob.cancel()
    }

    override suspend fun refresh() {
        states[currentState]?.invoke()
    }

    suspend fun on(condition: Boolean = true, body: suspend () -> Unit) {
        if (condition) {
            body()
        }
    }

    suspend fun on(duration: Duration, body: suspend () -> Unit) {
        delay(duration.toLongMilliseconds())
        on(body = body)
    }

    suspend fun entry(body: suspend () -> Unit) {
        if (currentState != previousState) {
            body()
        }
    }

    // todo: can we use generics here?
    operator fun Int?.compareTo(other: Int?): Int =
        if (this != null && other != null) this.compareTo(other)
        else -1
}

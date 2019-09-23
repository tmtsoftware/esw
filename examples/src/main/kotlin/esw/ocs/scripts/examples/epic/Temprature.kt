package esw.ocs.scripts.examples.epic

import esw.ocs.macros.StrandEc
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext
import kotlin.properties.Delegates
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.*
import java.util.concurrent.ExecutorService

interface State

object Init : State
object Ok : State
object High : State

interface Refreshable {
    suspend fun refresh(source: String)
}

abstract class Machine(val name: String, init: String) : CoroutineScope, Refreshable {

    private val _strandEc = StrandEc.apply()
    private val job = Job()
    private val dispatcher = _strandEc.executorService().asCoroutineDispatcher()

    override val coroutineContext: CoroutineContext
        get() = job + dispatcher

    private var currentState: String = init
    private var previousState: String? = null

    abstract suspend fun logic(state: String)

    protected fun become(state: String) {
        currentState = state
    }

    override suspend fun refresh(source: String) {
        println(
            "machine = $name%-8s    previousState = $previousState%-8s     currentState = $currentState%-8s    action = $source%-8s     ${debugString()}%8s"
        )
        logic(currentState)
    }

    suspend fun `when`(condition: Boolean = true, body: () -> Unit) {
        previousState = currentState
        if (condition) {
            body()
            refresh("when")
        }
    }

    @ExperimentalTime
    suspend fun `when`(duration: Duration, body: () -> Unit) {
        delay(duration.toLongMilliseconds())
        `when`(body = body)
    }

    fun entry(body: () -> Unit) {
        if (currentState != previousState) {
            body()
        }
    }

    open fun debugString(): String = ""
}

fun CoroutineScope.publishTemp(tmp: Int) {
    TODO()
}

fun CoroutineScope.subscribeTemp(name: String, tmp: suspend (Int) -> Unit) {
    TODO()
}

@UseExperimental(ExperimentalTime::class)
val machine1 = object : Machine("temp-monitor", "Init") {

    var temp: Int by Delegates.observable(0) { p, o, n ->
        publishTemp(n)
    }

    var temp2: Int by Delegates.observable(0) { p, o, n ->
        subscribeTemp("temp") {
            temp = it
        }
    }

    override suspend fun logic(state: String) {
        when (state) {
            "Init" -> {
                `when`() {
                    temp = 45
                    // temp.pvPut()
                    become("Ok")
                }
            }

            "Ok" -> {
                `when`(temp > 40) {
                    temp = 25
                    become("High")
                }
            }

            "High" -> {
                `when`(temp < 30) {
                    //                    temp.pvGet()
                    become("Ok")
                }
            }
        }
    }

    override fun debugString(): String = "temp2 = $temp"
}

fun main() = runBlocking {
    machine1.refresh("Init")

    delay(100000)
}

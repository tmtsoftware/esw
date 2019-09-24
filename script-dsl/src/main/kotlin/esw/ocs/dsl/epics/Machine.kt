package esw.ocs.dsl.epics

import csw.params.events.Event
import csw.params.events.EventKey
import esw.ocs.dsl.highlevel.EventServiceKtDsl
import esw.ocs.dsl.params.KeyHolder
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.time.Duration
import kotlinx.coroutines.*

abstract class Machine(private val name: String, init: String) : CoroutineScope,
    Refreshable, EventServiceKtDsl {

    private val ec = Executors.newSingleThreadScheduledExecutor()
    private val job = Job()
    private val dispatcher = ec.asCoroutineDispatcher()

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

    suspend fun `when`(condition: Boolean = true, body: suspend () -> Unit) {
        previousState = currentState
        if (condition) {
            body()
            refresh("when")
        }
    }

    suspend fun `when`(duration: Duration, body: suspend () -> Unit) {
        delay(duration.toLongMilliseconds())
        `when`(body = body)
    }

    fun entry(body: () -> Unit) {
        if (currentState != previousState) {
            body()
        }
    }

    fun <T> createVar(initial: T, eventKey: String, key: KeyHolder<T>) = Var(initial, eventKey, this, this, key)

    inline fun reactiveEvent(
        key: String,
        crossinline onChange: (property: KProperty<*>, oldValue: Event, newValue: Event) -> Unit
    ): ReadWriteProperty<Any?, Event> =
        object : ObservableProperty<Event>(Event.invalidEvent(EventKey.apply(key))) {
            override fun afterChange(property: KProperty<*>, oldValue: Event, newValue: Event) =
                onChange(property, oldValue, newValue)

            override fun getValue(thisRef: Any?, property: KProperty<*>): Event =
                runBlocking(coroutineContext) { getEvent(key).first() }

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: Event) =
                runBlocking(coroutineContext) {
                    publishEvent(value)
                    super.setValue(thisRef, property, value)
                }
        }

    open fun debugString(): String = ""
}

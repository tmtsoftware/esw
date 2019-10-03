package esw.ocs.dsl.epics

import csw.params.core.generics.Parameter
import csw.params.core.generics.ParameterSetType
import csw.params.events.EventKey
import csw.params.events.SystemEvent
import esw.ocs.dsl.highlevel.EventServiceDsl
import esw.ocs.dsl.params.KeyKt
import esw.ocs.dsl.utils.CswExtensions
import esw.ocs.dsl.utils.nullable
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.time.Duration
import kotlinx.coroutines.*

abstract class Machine(private val name: String, init: String) : CoroutineScope,
    Refreshable, EventServiceDsl, CswExtensions {

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
            "machine = $name    previousState = $previousState     currentState = $currentState    action = $source     ${debugString()}"
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

    fun <T> Var(initial: T, eventKey: String, key: KeyKt<T>) = Var(initial, eventKey, this, this, key)

    inline fun <T> reactiveEvent(
        initial: T,
        eventKey: String,
        keyKt: KeyKt<T>,
        crossinline onChange: (oldValue: T, newValue: T) -> Unit
    ): ReadWriteProperty<Any?, T> =
        object : ObservableProperty<T>(initial) {
            private val _eventKey = EventKey.apply(eventKey)

            init {
                runBlocking {
                    publishEvent(event(keyKt.set(initial)))
                }
            }

            // todo: should allow creating any type of event
            private fun event(param: Parameter<T>): SystemEvent =
                SystemEvent(_eventKey.source(), _eventKey.eventName()).add(param)

            override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) =
                onChange(oldValue, newValue)

            override fun getValue(thisRef: Any?, property: KProperty<*>): T =
                runBlocking(coroutineContext) {
                    val paramType: ParameterSetType<*>? = getEvent(eventKey).first().paramType()
                    paramType?.jGet(keyKt.key)?.nullable()?.jGet(0)?.nullable() ?: initial
                }

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) =
                runBlocking(coroutineContext) {
                    publishEvent(event(keyKt.set(value)))
                    super.setValue(thisRef, property, value)
                }
        }

    // todo: can we use generics here?
    operator fun Int?.compareTo(other: Int?): Int =
        if (this != null && other != null) this.compareTo(other)
        else -1

    open fun debugString(): String = ""
}

package esw.ocs.dsl.epics

import csw.params.core.generics.Key
import csw.params.core.generics.Parameter
import csw.params.events.Event
import csw.params.events.EventKey
import csw.params.events.SystemEvent
import esw.ocs.dsl.highlevel.EventServiceDsl
import esw.ocs.dsl.nullable
import esw.ocs.dsl.params.set

interface Refreshable {
    suspend fun refresh(source: String)
}

class Var<T> internal constructor(
    initial: T,
    private val eventKey: String,
    private val eventService: EventServiceDsl,
    private val refreshable: Refreshable,
    private val key: Key<T>
) {
    private val _eventKey = EventKey.apply(eventKey)
    private var _value: Event = event(key.set(initial))

    // todo: should allow creating any type of event
    private fun event(param: Parameter<T>): SystemEvent =
        SystemEvent(_eventKey.source(), _eventKey.eventName()).add(param)

    fun set(value: T) {
        _value = event(key.set(value))
    }

    fun get(): T? {
        val paramType = _value.paramType()
        return paramType.jGet(key).nullable()?.jGet(0)?.nullable()
    }

    suspend fun pvPut() {
        eventService.publishEvent(_value)
    }

    suspend fun pvGet() {
        val event = eventService.getEvent(eventKey).first()
        setValue(event, eventKey)
    }

    fun pvMonitor() =
        eventService.onEvent(eventKey) {
            setValue(it, eventKey)
        }

    private suspend fun setValue(value: Event, source: String) {
        _value = value
        refreshable.refresh(source)
    }

    override fun toString(): String = get().toString()
}

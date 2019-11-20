package esw.ocs.dsl.epics

import csw.params.core.generics.Key
import csw.params.core.generics.Parameter
import csw.params.events.Event
import csw.params.events.EventKey
import csw.params.events.SystemEvent
import esw.ocs.dsl.highlevel.EventServiceDsl
import esw.ocs.dsl.nullable

interface Refreshable {
    suspend fun refresh()
}

class Var<T> constructor(
        initial: T,
        private val eventKey: String,
        private val key: Key<T>,
        private val refreshable: Refreshable,
        private val eventService: EventServiceDsl
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
        setValue(event)
    }

    suspend fun pvMonitor() =
        eventService.onEvent(eventKey) {
            setValue(it)
        }

    private suspend fun setValue(value: Event) {
        _value = value
        refreshable.refresh()
    }

    override fun toString(): String = get().toString()
}

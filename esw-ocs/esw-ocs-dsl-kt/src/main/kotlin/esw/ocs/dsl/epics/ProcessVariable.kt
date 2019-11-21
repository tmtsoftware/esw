package esw.ocs.dsl.epics

import csw.params.core.generics.Key
import csw.params.events.Event
import csw.params.events.ObserveEvent
import csw.params.events.SystemEvent
import esw.ocs.dsl.highlevel.EventServiceDsl
import esw.ocs.dsl.nullable
import esw.ocs.dsl.params.set

interface Refreshable {
    fun refresh()
}

class ProcessVariable<T> internal constructor(
        initial: Event,
        private val key: Key<T>,
        private val eventService: EventServiceDsl
) {
    private val eventKey: String = initial.eventKey().key()
    private var latestEvent: Event = initial
    private val subscribers: Set<Refreshable> = mutableSetOf()

    suspend fun bind(refreshable: Refreshable) {
        subscribers + refreshable
        if (subscribers.size == 1) startSubscription()
    }

    suspend fun set(value: T) {
        val param = key.set(value)
        val oldEvent = latestEvent
        when (oldEvent) {
            is SystemEvent -> latestEvent = oldEvent.add(param)
            is ObserveEvent -> latestEvent = oldEvent.add(param)
        }
        eventService.publishEvent(latestEvent)
    }

    fun get(): T? = latestEvent.paramType().jGet(key).nullable()?.jGet(0)?.nullable()

    private suspend fun startSubscription() =
            eventService.onEvent(eventKey) { event ->
                latestEvent = event
                subscribers.forEach { it.refresh() }
            }
}

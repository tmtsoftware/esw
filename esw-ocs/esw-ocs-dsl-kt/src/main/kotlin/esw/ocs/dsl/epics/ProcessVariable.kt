package esw.ocs.dsl.epics

import csw.params.core.generics.Key
import csw.params.events.Event
import csw.params.events.ObserveEvent
import csw.params.events.SystemEvent
import esw.ocs.dsl.highlevel.EventServiceDsl
import esw.ocs.dsl.highlevel.Subscription
import esw.ocs.dsl.params.first
import esw.ocs.dsl.params.invoke

class ProcessVariable<T> constructor(
        initial: Event,
        private val key: Key<T>,
        private val eventService: EventServiceDsl
) {
    private val eventKey: String = initial.eventKey().key()

    // todo: should initial event be published?
    private var latestEvent: Event = initial
    private val subscribers: MutableSet<Refreshable> = mutableSetOf()

    suspend fun bind(refreshable: Refreshable) {
        subscribers.add(refreshable)
        if (subscribers.size == 1) {
            val eventSubscription = startSubscription()
            val subscription = FSMSubscription(eventSubscription) { subscribers -= refreshable }
            refreshable.addFSMSubscription(subscription)
        }
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

    // extract first value from a parameter against provided key from param set
    // if not present, throw an exception
    fun get(): T = (latestEvent.paramType())(key).first

    private suspend fun startSubscription(): Subscription =
            eventService.onEvent(eventKey) { event ->
                if (!event.isInvalid) {
                    latestEvent = event
                    subscribers.forEach { it.refresh() }
                }
            }
}

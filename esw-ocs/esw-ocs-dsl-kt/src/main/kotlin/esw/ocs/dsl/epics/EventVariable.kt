package esw.ocs.dsl.epics

import csw.params.core.generics.Key
import csw.params.core.generics.Parameter
import csw.params.events.Event
import csw.params.events.ObserveEvent
import csw.params.events.SystemEvent
import esw.ocs.dsl.highlevel.EventServiceDsl
import esw.ocs.dsl.highlevel.models.EventSubscription
import esw.ocs.dsl.params.first
import esw.ocs.dsl.params.invoke
import kotlin.time.Duration

class EventVariable<T> constructor(
        initial: Event,
        private val key: Key<T>,
        private val duration: Duration? = null,
        private val eventService: EventServiceDsl
) {
    private val eventKey: String = initial.eventKey().key()

    // todo: should initial event be published?
    private var latestEvent: Event = initial
    private val subscribers: MutableSet<Refreshable> = mutableSetOf()
    private var eventSubscription: EventSubscription? = null

    suspend fun bind(refreshable: Refreshable): FsmSubscription {
        subscribers.add(refreshable)
        if (subscribers.size == 1) eventSubscription = startSubscription()
        val fsmSubscription = FsmSubscription { unsubscribe(refreshable) }
        refreshable.addFsmSubscription(fsmSubscription)
        return fsmSubscription
    }

    fun getEvent() = latestEvent

    suspend fun setEvent(event: Event) = eventService.publishEvent(event)

    fun getParam(): Parameter<T> = (latestEvent.paramType())(key)

    // extract first value from a parameter against provided key from param set
    // if not present, throw an exception
    fun first(): T = getParam().first

    suspend fun setParam(value: T) {
        val param = key.set(value)
        val oldEvent = latestEvent
        when (oldEvent) {
            is SystemEvent -> latestEvent = oldEvent.add(param)
            is ObserveEvent -> latestEvent = oldEvent.add(param)
        }
        eventService.publishEvent(latestEvent)
    }


    private suspend fun startSubscription(): EventSubscription = if (duration != null) polling(duration) else subscribe()

    private suspend fun polling(duration: Duration): EventSubscription =
            eventService.onEvent(eventKey, duration = duration) {
                if (it != latestEvent) refresh(it)
            }

    private suspend fun subscribe(): EventSubscription = eventService.onEvent(eventKey) { refresh(it) }

    private suspend fun refresh(event: Event) {
        if (!event.isInvalid) {
            latestEvent = event
            subscribers.forEach { it.refresh() }
        }
    }

    private suspend fun unsubscribe(refreshable: Refreshable) {
        subscribers.remove(refreshable)
        if (subscribers.isEmpty()) eventSubscription?.cancel()
    }
}

package esw.ocs.dsl.epics

import akka.Done
import csw.params.core.generics.Key
import csw.params.core.generics.Parameter
import csw.params.events.Event
import csw.params.events.EventKey
import csw.params.events.SystemEvent
import esw.ocs.dsl.add
import esw.ocs.dsl.highlevel.EventServiceDsl
import esw.ocs.dsl.highlevel.models.EventSubscription
import esw.ocs.dsl.params.first
import esw.ocs.dsl.params.invoke
import kotlin.time.Duration

interface EventVariable {
    suspend fun bind(refreshable: Refreshable): FsmSubscription
    fun getEvent(): Event
}

interface ParamVariable<T> : EventVariable {
    fun first(): T
    fun getParam(): Parameter<T>
    suspend fun setParam(value: T): Done
}

class EventVariableImpl<T> private constructor(
        initial: Event,
        private val eventService: EventServiceDsl,
        private val key: Key<T>? = null,
        private val duration: Duration? = null
) : ParamVariable<T> {
    private val eventKey: String = initial.eventKey().key()

    private var latestEvent: Event = initial
    private val subscribers: MutableSet<Refreshable> = mutableSetOf()
    private var eventSubscription: EventSubscription? = null

    override suspend fun bind(refreshable: Refreshable): FsmSubscription {
        subscribers.add(refreshable)
        if (subscribers.size == 1) eventSubscription = startSubscription()
        val fsmSubscription = FsmSubscription { unsubscribe(refreshable) }
        refreshable.addFsmSubscription(fsmSubscription)
        return fsmSubscription
    }

    override fun getEvent(): Event = latestEvent

    // This is method is supposed to be used only from ParamVariable, so it can be safely
    // assumed that Key will be present while calling it.
    override fun getParam(): Parameter<T> = key?.let { (latestEvent.paramType()).invoke(it) } ?: throwKeyNotGiven()

    // extract first value from a parameter against provided key from param set
    // if not present, throw an exception
    override fun first(): T = getParam().first

    // This is method is supposed to be used only from ParamVariable, so it can be safely
    // assumed that Key will be present while calling it.
    override suspend fun setParam(value: T): Done {
        val param: Parameter<T> = key?.set(value) ?: throwKeyNotGiven()
        return eventService.publishEvent(latestEvent.add(param))
    }

    private fun throwKeyNotGiven(): Nothing =
            throw RuntimeException("EventVariable cannot be use this method, as it ties to Event and not to any specific Parameter")

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

    companion object {

        private fun createInitialEvent(eventKeyStr: String): Event {
            val eventKey = EventKey.apply(eventKeyStr)
            return SystemEvent(eventKey.source(), eventKey.eventName())
        }

        fun createEventVariable(eventKeyStr: String, eventService: EventServiceDsl, duration: Duration? = null): EventVariable {
            val initialEvent = createInitialEvent(eventKeyStr)
            return EventVariableImpl<Unit>(initialEvent, eventService, duration = duration)
        }

        fun <T> createParamVariable(initialValue: T, key: Key<T>, eventKeyStr: String, eventService: EventServiceDsl, duration: Duration? = null): ParamVariable<T> {
            val initialEvent = createInitialEvent(eventKeyStr)
            return EventVariableImpl(initialEvent.add(key.set(initialValue)), eventService, key, duration)
        }
    }

}

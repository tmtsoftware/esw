package esw.ocs.dsl.epics

import org.apache.pekko.Done
import csw.params.core.generics.Key
import csw.params.core.generics.Parameter
import csw.params.events.Event
import csw.params.events.EventKey
import esw.ocs.dsl.add
import esw.ocs.dsl.highlevel.CswHighLevelDslApi
import esw.ocs.dsl.highlevel.models.EventSubscription
import esw.ocs.dsl.params.first
import esw.ocs.dsl.params.invoke
import esw.ocs.dsl.params.values
import kotlin.time.Duration

/**
 * This class is a wrapper class for an Event param which provides method to subscribe to the EventKey of initial Event
 * and provides the method to get the latest event for that eventKey
 *
 * @property cswApi - an instance of CswHighLevelDslApi
 * @property duration - duration of polling for latest event
 * @constructor
 *
 * @param initial - initial event
 */
open class EventVariable protected constructor(
        initial: Event,
        private val cswApi: CswHighLevelDslApi,
        private val duration: Duration? = null
) {
    private val eventKey: String = initial.eventKey().key()

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

    fun getEvent(): Event = latestEvent

    private suspend fun startSubscription(): EventSubscription = if (duration != null) polling(duration) else subscribe()

    private suspend fun polling(duration: Duration): EventSubscription {
        val cancellable = cswApi.schedulePeriodically(duration) {
            cswApi.getEvent(eventKey).let { if (it != latestEvent) refresh(it) }
        }

        return EventSubscription { cancellable.cancel() }
    }

    private suspend fun subscribe(): EventSubscription = cswApi.onEvent(eventKey) { refresh(it) }

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
        suspend fun make(eventKey: EventKey, cswApi: CswHighLevelDslApi, duration: Duration? = null): EventVariable {
            val initial = cswApi.getEvent(eventKey.key())
            return EventVariable(initial, cswApi, duration)
        }
    }
}

/**
 * An extension class on EventVariable which provides method to work of  the event's(event in EventVariable) paramSet
 *
 * @param T - type of Param
 * @property key - param key
 * @property cswApi - an instance of CswHighLevelDslApi
 * @constructor
 *
 * @param initial - initial event
 * @param duration - duration of polling for latest event
 */
class ParamVariable<T> private constructor(
        initial: Event,
        private val key: Key<T>,
        private val cswApi: CswHighLevelDslApi,
        duration: Duration? = null
) : EventVariable(initial, cswApi, duration) {

    fun getParam(): Parameter<T> = (getEvent().paramType()).invoke(key)

    // extract first value from a parameter against provided key from param set
    // if not present, throw an exception
    fun first(): T = getParam().first

    // extract the values of a parameter as a list
    fun values(): List<T> = getParam().values

    suspend fun setParam(vararg value: T): Done = cswApi.publishEvent(getEvent().add(key.setAll(value)))

    companion object {
        suspend fun <T> make(initial: T, key: Key<T>, eventKey: EventKey, cswApi: CswHighLevelDslApi, duration: Duration? = null): ParamVariable<T> {
            val availableEvent = cswApi.getEvent(eventKey.key())
            val initialEvent = availableEvent.add(key.set(initial))
            cswApi.publishEvent(initialEvent)

            return ParamVariable(initialEvent, key, cswApi, duration)
        }
    }
}

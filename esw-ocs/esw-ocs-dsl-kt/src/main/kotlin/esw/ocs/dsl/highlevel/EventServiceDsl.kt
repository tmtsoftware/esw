package esw.ocs.dsl.highlevel

import akka.Done
import akka.actor.Cancellable
import csw.event.api.javadsl.IEventPublisher
import csw.event.api.javadsl.IEventSubscriber
import csw.params.core.generics.Key
import csw.params.core.generics.Parameter
import csw.params.core.models.Prefix
import csw.params.events.*
import esw.ocs.dsl.SuspendableConsumer
import esw.ocs.dsl.SuspendableSupplier
import esw.ocs.dsl.epics.EventVariable
import esw.ocs.dsl.params.set
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import java.util.*
import kotlin.time.Duration
import kotlin.time.toJavaDuration

data class EventSubscription(private val unsubscribe: suspend () -> Unit) {
    suspend fun cancel() = unsubscribe() // to solve mocking issue, we had to introduce this function: https://github.com/mockk/mockk/issues/288
}

interface EventServiceDsl {
    val coroutineScope: CoroutineScope
    val defaultPublisher: IEventPublisher
    val defaultSubscriber: IEventSubscriber

    fun EventKey(prefix: String, eventName: String): EventKey = EventKey(Prefix.apply(prefix), EventName(eventName))
    fun EventKey(eventKeyStr: String): EventKey = EventKey.apply(eventKeyStr)

    fun SystemEvent(sourcePrefix: String, eventName: String, vararg parameters: Parameter<*>): SystemEvent =
            SystemEvent(Prefix.apply(sourcePrefix), EventName(eventName)).jMadd(parameters.toSet())

    fun ObserveEvent(sourcePrefix: String, eventName: String, vararg parameters: Parameter<*>): ObserveEvent =
            ObserveEvent(Prefix.apply(sourcePrefix), EventName(eventName)).jMadd(parameters.toSet())

    suspend fun publishEvent(event: Event): Done = defaultPublisher.publish(event).await()

    fun publishEvent(every: Duration, eventGenerator: SuspendableSupplier<Event?>): Cancellable =
            defaultPublisher.publishAsync({
                coroutineScope.future { Optional.ofNullable(eventGenerator()) }
            }, every.toJavaDuration())

    suspend fun onEvent(vararg eventKeys: String, callback: SuspendableConsumer<Event>): EventSubscription {
        val subscription = defaultSubscriber.subscribeAsync(eventKeys.toEventKeys()) { coroutineScope.future { callback(it) } }
        subscription.ready().await()
        return EventSubscription { subscription.unsubscribe().await() }
    }

    suspend fun getEvent(vararg eventKeys: String): Set<Event> =
            defaultSubscriber.get(eventKeys.toEventKeys()).await().toSet()

    suspend fun <T> SystemVar(initial: T, eventKeyStr: String, key: Key<T>, duration: Duration? = null): EventVariable<T> {
        val eventKey = EventKey(eventKeyStr)
        val systemEvent = SystemEvent(eventKey.source().value(), eventKey.eventName().name(), key.set(initial))
        return EventVariable(systemEvent, key, duration, this)
    }

    suspend fun <T> ObserveVar(initial: T, eventKeyStr: String, key: Key<T>, duration: Duration? = null): EventVariable<T> {
        val eventKey = EventKey(eventKeyStr)
        val observeEvent = ObserveEvent(eventKey.source().value(), eventKey.eventName().name(), key.set(initial))
        return EventVariable(observeEvent, key, duration, this)
    }

    private fun (Array<out String>).toEventKeys(): Set<EventKey> = map { EventKey.apply(it) }.toSet()
}

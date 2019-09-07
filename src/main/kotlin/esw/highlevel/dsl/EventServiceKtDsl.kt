package esw.highlevel.dsl

import akka.Done
import akka.actor.Cancellable
import csw.event.api.javadsl.IEventSubscription
import csw.params.core.generics.Parameter
import csw.params.events.Event
import csw.params.events.ObserveEvent
import csw.params.events.SystemEvent
import esw.ocs.dsl.CswServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import java.time.Duration
import java.util.*

interface EventServiceKtDsl : CoroutineScope {
    val cswServices: CswServices

    fun systemEvent(sourcePrefix: String, eventName: String, vararg parameters: Parameter<*>): SystemEvent =
        cswServices.jSystemEvent(sourcePrefix, eventName, parameters.toSet())

    fun observeEvent(sourcePrefix: String, eventName: String, vararg parameters: Parameter<*>): ObserveEvent =
        cswServices.jObserveEvent(sourcePrefix, eventName, parameters.toSet())

    suspend fun publishEvent(event: Event): Done =
        cswServices.jPublishEvent(event).await()

    fun publishEvent(every: Duration, eventGenerator: suspend () -> Optional<Event>): Cancellable =
        cswServices.jPublishEventAsync(every) { future { eventGenerator() } }

    fun onEvent(vararg eventKeys: String, callback: suspend (Event) -> Unit): IEventSubscription =
        cswServices.jOnEvent(eventKeys.toSet()) { future { callback(it) } }

    suspend fun getEvent(vararg eventKeys: String): Set<Event> =
        cswServices.jGetEvent(eventKeys.toSet()).await().toSet()

}

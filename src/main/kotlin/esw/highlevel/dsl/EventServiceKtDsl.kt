package esw.highlevel.dsl

import akka.Done
import akka.actor.Cancellable
import csw.event.api.scaladsl.EventSubscription
import csw.params.core.generics.Parameter
import csw.params.events.Event
import csw.params.events.ObserveEvent
import csw.params.events.SystemEvent
import esw.ocs.dsl.CswServices
import esw.ocs.macros.StrandEc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletionStage
import java.util.function.Supplier

interface EventServiceKtDsl : CoroutineScope {
    val cswServices: CswServices
    fun strandEc(): StrandEc

    fun systemEvent(sourcePrefix: String, eventName: String, vararg parameters: Parameter<*>): SystemEvent =
        cswServices.jSystemEvent(sourcePrefix, eventName, parameters.toSet())

    fun observeEvent(sourcePrefix: String, eventName: String, vararg parameters: Parameter<*>): ObserveEvent =
        cswServices.jObserveEvent(sourcePrefix, eventName, parameters.toSet())

    suspend fun publishEvent(event: Event): Done = cswServices.jPublishEvent(event).await()

    // todo: see if this works and simplify
    suspend fun publishEvent(every: Duration, eventGenerator: suspend () -> Optional<Event>): Cancellable {
        val xx: Supplier<CompletionStage<Optional<Event>>> = coroutineScope {
            Supplier {
                future {
                    eventGenerator()
                }.minimalCompletionStage()
            }
        }

        return cswServices.jPublishEventAsync(every, xx, strandEc())
    }

    fun onEvent(vararg eventKeys: String, callback: (Event) -> Unit): EventSubscription =
        cswServices.jOnEvent(eventKeys.toSet(), callback, strandEc())

    suspend fun getEvent(vararg eventKeys: String): Set<Event> =
        cswServices.jGetEvent(eventKeys.toSet(), strandEc()).await().toSet()

}

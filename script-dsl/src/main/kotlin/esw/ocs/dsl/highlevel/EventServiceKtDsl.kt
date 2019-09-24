package esw.ocs.dsl.highlevel

import akka.Done
import akka.actor.Cancellable
import csw.event.api.javadsl.IEventPublisher
import csw.event.api.javadsl.IEventService
import csw.event.api.javadsl.IEventSubscriber
import csw.event.api.javadsl.IEventSubscription
import csw.params.core.generics.Parameter
import csw.params.core.models.Prefix
import csw.params.events.*
import java.util.*
import kotlin.time.Duration
import kotlin.time.toJavaDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future

interface EventServiceKtDsl : CoroutineScope {
    val eventService: IEventService

    private val defaultPublisher: IEventPublisher
        get() = eventService.defaultPublisher()

    private val defaultSubscriber: IEventSubscriber
        get() = eventService.defaultSubscriber()

    fun systemEvent(sourcePrefix: String, eventName: String, vararg parameters: Parameter<*>): SystemEvent =
        SystemEvent(Prefix(sourcePrefix), EventName(eventName)).jMadd(parameters.toSet())

    fun observeEvent(sourcePrefix: String, eventName: String, vararg parameters: Parameter<*>): ObserveEvent =
        ObserveEvent(Prefix(sourcePrefix), EventName(eventName)).jMadd(parameters.toSet())

    suspend fun publishEvent(event: Event): Done = defaultPublisher.publish(event).await()

    fun publishEvent(every: Duration, eventGenerator: suspend () -> Event?): Cancellable =
        defaultPublisher.publishAsync({
            future { Optional.ofNullable(eventGenerator()) }
        }, every.toJavaDuration())

    fun onEvent(vararg eventKeys: String, callback: suspend (Event) -> Unit): IEventSubscription =
        defaultSubscriber.subscribeAsync(eventKeys.toEventKeys()) { future { callback(it) } }

    suspend fun getEvent(vararg eventKeys: String): Set<Event> =
        defaultSubscriber.get(eventKeys.toEventKeys()).await().toSet()

    private fun (Array<out String>).toEventKeys(): Set<EventKey> = map { EventKey.apply(it) }.toSet()
}

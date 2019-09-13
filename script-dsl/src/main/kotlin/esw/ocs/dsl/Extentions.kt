package esw.ocs.dsl

import akka.Done
import csw.event.api.javadsl.IEventSubscription
import kotlinx.coroutines.future.await
import java.util.*

fun <T> Optional<T>.nullable(): T? = orElse(null)

suspend fun IEventSubscription.cancel(): Done = unsubscribe().await()
suspend fun IEventSubscription.completed(): Done = ready().await()
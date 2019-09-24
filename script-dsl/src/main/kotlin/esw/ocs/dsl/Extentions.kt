package esw.ocs.dsl

import akka.Done
import csw.event.api.javadsl.IEventSubscription
import java.util.*
import kotlinx.coroutines.future.await

fun <T> Optional<T>.nullable(): T? = orElse(null)

// todo: can we use generics here?
operator fun Int?.compareTo(other: Int?): Int =
    if (this != null && other != null) this.compareTo(other)
    else -1

suspend fun IEventSubscription.cancel(): Done = unsubscribe().await()
suspend fun IEventSubscription.completed(): Done = ready().await()

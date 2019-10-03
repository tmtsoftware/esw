package esw.ocs.dsl.utils

import java.util.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

suspend fun <T> par(vararg tasks: suspend () -> T): List<T> = coroutineScope {
    val deferreds = tasks.map { async { it() } }
    deferreds.awaitAll()
}

fun <T> Optional<T>.nullable(): T? = orElse(null)

package esw.ocs.dsl

import java.util.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

fun <T> Optional<T>.nullable(): T? = orElse(null)

interface Dsl {

    suspend fun <T> par(vararg tasks: suspend () -> T): List<T> = coroutineScope {
        val deferreds = tasks.map { async { it() } }
        deferreds.awaitAll()
    }
}

package esw.ocs.dsl.internal

import java.util.*

fun <T> Optional<T>.nullable(): T? = orElse(null)

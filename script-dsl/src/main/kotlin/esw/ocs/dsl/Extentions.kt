package esw.ocs.dsl

import java.util.*

fun <T> Optional<T>.nullable(): T? = orElse(null)

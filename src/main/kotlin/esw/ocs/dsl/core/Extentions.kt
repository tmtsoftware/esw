package esw.ocs.dsl.core

import java.util.*

fun <T> Optional<T>.nullable(): T? = orElse(null)

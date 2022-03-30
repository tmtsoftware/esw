/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.dsl

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Executes provided tasks in parallel and waits for all of them to complete
 * @param tasks varargs of suspending tasks
 * @return list of responses when all the tasks are completed
 */
suspend fun <T> par(vararg tasks: suspend () -> T): List<T> = coroutineScope {
    val deferreds = tasks.map { async { it() } }
    deferreds.awaitAll()
}

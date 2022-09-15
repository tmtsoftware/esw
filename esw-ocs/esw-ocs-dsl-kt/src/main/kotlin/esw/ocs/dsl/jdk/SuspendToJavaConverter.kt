/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.dsl.jdk

import esw.ocs.dsl.SuspendableCallback
import esw.ocs.dsl.SuspendableConsumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.launch
import java.util.concurrent.CompletionStage

interface SuspendToJavaConverter {
    val coroutineScope: CoroutineScope

    fun SuspendableCallback.toJava(_coroutineScope: CoroutineScope = coroutineScope): CompletionStage<Void> =
            _coroutineScope.launch { this@toJava() }.asCompletableFuture().thenAccept { }

    fun <T> (SuspendableConsumer<T>).toJava(value: T): CompletionStage<Void> {
        val curriedBlock: suspend (CoroutineScope) -> Unit = { a: CoroutineScope -> this(a, value) }
        return curriedBlock.toJava()
    }
}


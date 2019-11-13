package esw.ocs.dsl.jdk

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.launch
import java.util.concurrent.CompletionStage

interface SuspendToJavaConverter {
    val coroutineScope: CoroutineScope

    fun (suspend CoroutineScope.() -> Unit).toJavaFutureVoid(): CompletionStage<Void> =
            coroutineScope.launch { this@toJavaFutureVoid() }.asCompletableFuture().thenAccept { }

    fun <T> (suspend CoroutineScope.(T) -> Unit).toJavaFuture(value: T): CompletionStage<Void> {
        val curriedBlock: suspend (CoroutineScope) -> Unit = { a: CoroutineScope -> this(a, value) }
        return curriedBlock.toJavaFutureVoid()
    }
}


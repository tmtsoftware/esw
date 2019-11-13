package esw.ocs.dsl.jdk

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.launch
import java.util.concurrent.CompletionStage

interface SuspendToJavaConverter {
    val coroutineScope: CoroutineScope

    fun (suspend CoroutineScope.() -> Unit).toJava(): CompletionStage<Void> =
            coroutineScope.launch { this@toJava() }.asCompletableFuture().thenAccept { }

    fun <T> (suspend CoroutineScope.(T) -> Unit).toJava(value: T): CompletionStage<Void> {
        val curriedBlock: suspend (CoroutineScope) -> Unit = { a: CoroutineScope -> this(a, value) }
        return curriedBlock.toJava()
    }
}


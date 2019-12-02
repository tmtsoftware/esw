package esw.ocs.dsl.core

import csw.params.commands.SequenceCommand
import esw.ocs.dsl.jdk.SuspendToJavaConverter
import esw.ocs.dsl.script.SequenceCommandHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.launch
import java.util.concurrent.CompletionStage

class SequenceCommandHandlerKt<T : SequenceCommand>(
        private val block: suspend CoroutineScope.(T) -> Unit,
        override val coroutineScope: CoroutineScope
) : SequenceCommandHandler<T>, SuspendToJavaConverter {

    private var retryCount: Int = 0
    private var onError: (suspend CoroutineScope.(Throwable) -> Unit)? = null

    override fun execute(sequenceCommand: T): CompletionStage<Void> =
            coroutineScope.launch {
                suspend fun go(): Unit =
                        try {
                            block(sequenceCommand)
                        } catch (e: Exception) {
                            onError?.let { it(e) }
                            if (retryCount > 0) {
                                retryCount -= 1
                                go()
                            } else throw e
                        }

                go()
            }.asCompletableFuture().thenAccept { }

    fun onError(block: suspend CoroutineScope.(Throwable) -> Unit): SequenceCommandHandlerKt<T> {
        onError = block
        return this
    }

    fun retry(count: Int) {
        retryCount = count
    }

}
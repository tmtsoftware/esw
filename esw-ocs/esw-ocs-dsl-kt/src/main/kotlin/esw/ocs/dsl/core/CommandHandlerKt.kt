package esw.ocs.dsl.core

import csw.params.commands.SequenceCommand
import esw.ocs.dsl.SuspendableConsumer
import esw.ocs.dsl.highlevel.models.ScriptError
import esw.ocs.dsl.script.CommandHandler
import esw.ocs.dsl.toScriptError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.launch
import java.util.concurrent.CompletionStage
import kotlin.time.Duration

/**
 * Base trait is defined at scala side, which is used to install command handlers in ScriptDsl
 * We want ability to add error handlers and retries on onSetup/onObserve command handlers
 * Having implementation at kotlin side, allows us to execute onError or retry handlers inside same parent coroutine
 * Hence if exception gets thrown even after all the retries, this gets propagated to top-level exception handler
 */
class CommandHandlerKt<T : SequenceCommand>(
        private val scope: CoroutineScope,
        private val commandHandlerScope: CommandHandlerScope,
        private val block: SuspendableConsumer<T>
) : CommandHandler<T> {

    private var retryCount: Int = 0
    private var onError: (suspend CommandHandlerScope.(ScriptError) -> Unit)? = null
    private var delayInMillis: Long = 0

    override fun execute(sequenceCommand: T): CompletionStage<Void> {
        var localRetryCount = retryCount

        return scope.launch {
            suspend fun go(): Unit =
                    try {
                        block(sequenceCommand)
                    } catch (e: Exception) {
                        onError?.let { it(commandHandlerScope, e.toScriptError()) }
                        if (localRetryCount > 0) {
                            localRetryCount -= 1
                            delay(delayInMillis)
                            go()
                        } else throw e
                    }

            go()
        }.asCompletableFuture().thenAccept { }
    }

    fun onError(block: suspend CommandHandlerScope.(ScriptError) -> Unit): CommandHandlerKt<T> {
        onError = block
        return this
    }

    fun retry(count: Int) {
        retryCount = count
    }

    fun retry(count: Int, interval: Duration) {
        retry(count)
        delayInMillis = interval.toLongMilliseconds()
    }

}

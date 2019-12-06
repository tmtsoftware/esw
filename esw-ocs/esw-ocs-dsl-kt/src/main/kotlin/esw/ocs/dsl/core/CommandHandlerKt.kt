package esw.ocs.dsl.core

import csw.params.commands.SequenceCommand
import esw.ocs.dsl.SuspendableConsumer
import esw.ocs.dsl.highlevel.ScriptError
import esw.ocs.dsl.highlevel.OtherError
import esw.ocs.dsl.highlevel.SubmitError
import esw.ocs.dsl.internal.toScriptError
import esw.ocs.dsl.script.CommandHandler
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
        private val block: SuspendableConsumer<T>
) : CommandHandler<T> {

    private var retryCount: Int = 0
    private var onError: (SuspendableConsumer<ScriptError>)? = null
    private var delayInMillis: Long = 0

    override fun execute(sequenceCommand: T): CompletionStage<Void> =
            scope.launch {
                suspend fun go(): Unit =
                        try {
                            block(sequenceCommand)
                        } catch (e: Exception) {
                            onError?.let { it(e.toScriptError()) }
                            if (retryCount > 0) {
                                retryCount -= 1
                                delay(delayInMillis)
                                go()
                            } else throw e
                        }

                go()
            }.asCompletableFuture().thenAccept { }

    fun onError(block: SuspendableConsumer<ScriptError>): CommandHandlerKt<T> {
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
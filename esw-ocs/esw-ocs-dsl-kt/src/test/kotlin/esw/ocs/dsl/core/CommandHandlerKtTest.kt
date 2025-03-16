package esw.ocs.dsl.core

import csw.params.commands.Setup
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration.Companion.seconds

@Suppress("DANGEROUS_CHARACTERS")
internal class CommandHandlerKtTest {
    private val exceptionHandler = CoroutineExceptionHandler { _, _ -> } // to swallow all the test exceptions
    private fun scope() = CoroutineScope(EmptyCoroutineContext + exceptionHandler)

    private val commandHandlerScope = mockk<CommandHandlerScope>()

    @Nested
    inner class OnError {
        @Test
        fun `should_be_called_when_command_handler_throws_an_exception_|_ESW-249`(): Unit = runBlocking {
            var errorHandlerCounter = 0
            var commandHandlerCounter = 0

            val sequenceCommand = mockk<Setup>()

            val commandHandlerKt: CommandHandlerKt<Setup> =
                    CommandHandlerKt<Setup>(scope(), commandHandlerScope) {
                        commandHandlerCounter++
                        throw RuntimeException("exception")
                    }.onError { errorHandlerCounter++ }

            shouldThrow<RuntimeException> { commandHandlerKt.execute(sequenceCommand).await() }

            commandHandlerCounter shouldBe 1
            errorHandlerCounter shouldBe 1
        }

        @Test
        fun `should_not_be_called_when_command_handler_does_not_throw_an_exception_|_ESW-249`(): Unit = runBlocking {
            var errorHandlerCounter = 0
            var commandHandlerCounter = 0

            val sequenceCommand = mockk<Setup>()

            val commandHandlerKt: CommandHandlerKt<Setup> =
                    CommandHandlerKt<Setup>(scope(), commandHandlerScope) {
                        commandHandlerCounter += 1
                    }.onError { errorHandlerCounter++ }

            shouldNotThrow<Exception> { commandHandlerKt.execute(sequenceCommand).await() }

            commandHandlerCounter shouldBe 1
            errorHandlerCounter shouldBe 0
        }
    }

    @Nested
    inner class Retry {
        @Test
        fun `should_retry_command_handler_until_it_passes_|_ESW-249`(): Unit = runBlocking {
            var errorHandlerCounter = 0
            var commandHandlerCounter = 0

            val sequenceCommand = mockk<Setup>()

            val commandHandlerKt: CommandHandlerKt<Setup> =
                    CommandHandlerKt<Setup>(scope(), commandHandlerScope) {
                        commandHandlerCounter++
                        if (commandHandlerCounter < 3) throw RuntimeException("exception")
                    }.onError { errorHandlerCounter++ }
            commandHandlerKt.retry(2)

            shouldNotThrow<RuntimeException> { commandHandlerKt.execute(sequenceCommand).await() }

            commandHandlerCounter shouldBe 3
            errorHandlerCounter shouldBe 2

            // Assert that the original retry count is used for executing successive commands
            errorHandlerCounter = 0
            commandHandlerCounter = 0

            shouldNotThrow<RuntimeException> { commandHandlerKt.execute(sequenceCommand).await() }

            commandHandlerCounter shouldBe 3
            errorHandlerCounter shouldBe 2
        }

        @Test
        fun `should_retry_command_handler_until_retry_count_becomes_0_|_ESW-249`(): Unit = runBlocking {
            var errorHandlerCounter = 0
            var commandHandlerCounter = 0

            val sequenceCommand = mockk<Setup>()

            val commandHandlerKt: CommandHandlerKt<Setup> = CommandHandlerKt<Setup>(scope(), commandHandlerScope) {
                commandHandlerCounter++
                throw RuntimeException("exception")
            }.onError { errorHandlerCounter++ }

            commandHandlerKt.retry(2)

            shouldThrow<RuntimeException> { commandHandlerKt.execute(sequenceCommand).await() }

            commandHandlerCounter shouldBe 3
            errorHandlerCounter shouldBe 3
        }

        @Test
        fun `should_retry_command_handler_after_given_interval_|_ESW-249`(): Unit = runBlocking {
            var errorHandlerCounter = 0
            var commandHandlerCounter = 0

            val sequenceCommand = mockk<Setup>()

            val commandHandlerKt: CommandHandlerKt<Setup> =
                    CommandHandlerKt<Setup>(scope(), commandHandlerScope) {
                        commandHandlerCounter++
                        if (commandHandlerCounter < 2) throw RuntimeException("exception")
                    }.onError { errorHandlerCounter++ }

            commandHandlerKt.retry(2, 1.seconds)
            commandHandlerKt.execute(sequenceCommand)

            delay(100)
            commandHandlerCounter shouldBe 1
            errorHandlerCounter shouldBe 1

            delay(500)
            commandHandlerCounter shouldBe 1

            delay(500)
            commandHandlerCounter shouldBe 2
        }
    }
}

package esw.ocs.dsl.core

import csw.params.commands.Setup
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotThrow
import io.kotlintest.shouldThrow
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.coroutines.EmptyCoroutineContext

internal class CommandHandlerKtTest {
    private fun scope() = CoroutineScope(EmptyCoroutineContext)

    @Nested
    inner class OnError {
        @Test
        fun `should be called when command handler throws an exception | ESW-249`() = runBlocking {
            var errorHandlerCounter = 0
            var commandHandlerCounter = 0

            val sequenceCommand = mockk<Setup>()

            val commandHandlerKt: CommandHandlerKt<Setup> =
                    CommandHandlerKt<Setup>(scope()) {
                        commandHandlerCounter++
                        throw RuntimeException("exception")
                    }.onError { errorHandlerCounter++ }

            shouldThrow<RuntimeException> { commandHandlerKt.execute(sequenceCommand).await() }

            commandHandlerCounter shouldBe 1
            errorHandlerCounter shouldBe 1
        }

        @Test
        fun `should not be called when command handler does not throw an exception | ESW-249`() = runBlocking {
            var errorHandlerCounter = 0
            var commandHandlerCounter = 0

            val sequenceCommand = mockk<Setup>()

            val commandHandlerKt: CommandHandlerKt<Setup> =
                    CommandHandlerKt<Setup>(scope()) {
                        println(commandHandlerCounter)
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
        fun `should retry command handler until it passes | ESW-249`() = runBlocking {
            var errorHandlerCounter = 0
            var commandHandlerCounter = 0

            val sequenceCommand = mockk<Setup>()

            val commandHandlerKt: CommandHandlerKt<Setup> =
                    CommandHandlerKt<Setup>(scope()) {
                        commandHandlerCounter++
                        if (commandHandlerCounter < 2) throw RuntimeException("exception")
                    }.onError { errorHandlerCounter++ }
            commandHandlerKt.retry(2)

            shouldNotThrow<RuntimeException> { commandHandlerKt.execute(sequenceCommand).await() }

            commandHandlerCounter shouldBe 2
            errorHandlerCounter shouldBe 1
        }

        @Test
        fun `should retry command handler until retry count becomes 0 | ESW-249`() = runBlocking {
            var errorHandlerCounter = 0
            var commandHandlerCounter = 0

            val sequenceCommand = mockk<Setup>()

            val commandHandlerKt: CommandHandlerKt<Setup> = CommandHandlerKt<Setup>(scope()) {
                commandHandlerCounter++
                throw RuntimeException("exception")
            }.onError { errorHandlerCounter++ }

            commandHandlerKt.retry(2)

            shouldThrow<RuntimeException> { commandHandlerKt.execute(sequenceCommand).await() }

            commandHandlerCounter shouldBe 3
            errorHandlerCounter shouldBe 3
        }
    }
}
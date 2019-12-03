package esw.ocs.dsl.core

import csw.params.commands.Setup
import esw.ocs.dsl.SuspendableConsumer
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotThrow
import io.kotlintest.shouldThrow
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.coroutines.EmptyCoroutineContext

internal class CommandHandlerKtTest {

    private val coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)

    @Test
    fun `error handler should be called if command handler throws an exception`() = runBlocking {
        var errorHandlerCounter = 0
        var commandHandlerCounter = 0

        val sequenceCommand = mockk<Setup>()

        val commandHandler: SuspendableConsumer<Setup> = { _ ->
            commandHandlerCounter++
            throw RuntimeException("exception")
        }

        val commandHandlerKt: CommandHandlerKt<Setup> = CommandHandlerKt(commandHandler, coroutineScope)
        commandHandlerKt.onError { errorHandlerCounter++ }

        shouldThrow<RuntimeException> { commandHandlerKt.execute(sequenceCommand).await() }

        commandHandlerCounter shouldBe 1
        errorHandlerCounter shouldBe 1
    }

    @Test
    fun `command handler should be retry until it passes or retry count becomes 0`() = runBlocking {
        var errorHandlerCounter = 0
        var commandHandlerCounter = 0

        val sequenceCommand = mockk<Setup>()

        val commandHandler: SuspendableConsumer<Setup> = { _ ->
            commandHandlerCounter++
            if (commandHandlerCounter < 2) throw RuntimeException("exception")
        }

        val commandHandlerKt: CommandHandlerKt<Setup> = CommandHandlerKt(commandHandler, coroutineScope)
        commandHandlerKt.onError { errorHandlerCounter++ }
        commandHandlerKt.retry(2)

        shouldNotThrow<RuntimeException> { commandHandlerKt.execute(sequenceCommand).await() }

        commandHandlerCounter shouldBe 2
        errorHandlerCounter shouldBe 1
    }
}
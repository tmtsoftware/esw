package esw.ocs.dsl.highlevel

import csw.params.commands.CommandName
import csw.params.commands.Observe
import csw.params.commands.Setup
import csw.params.core.models.ObsId
import csw.params.core.models.Prefix
import io.kotlintest.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.*

class CommandServiceDslTest : CommandServiceDsl {

    @Test
    fun `setup method should construct a Setup command with given prefix, commandName and obsId | ESW-121`() = runBlocking {
        val actualSetupCommand: Setup = setup("esw.test", "move", "testObsId")
        val expectedSetupCommand = Setup(Prefix("esw.test"), CommandName("move"), Optional.of(ObsId("testObsId")))

        actualSetupCommand.source() shouldBe expectedSetupCommand.source()
        actualSetupCommand.commandName() shouldBe expectedSetupCommand.commandName()
        actualSetupCommand.maybeObsId() shouldBe expectedSetupCommand.maybeObsId()
    }

    @Test
    fun `observe method should construct a Observe command with given prefix, commandName and obsId | ESW-121`() = runBlocking {
        val expectedObserveCommand = Observe(Prefix("esw.test"), CommandName("move"), Optional.of(ObsId("testObsId")))
        val actualObserveCommand: Observe = observe("esw.test", "move", "testObsId")
        actualObserveCommand.source() shouldBe expectedObserveCommand.source()
        actualObserveCommand.commandName() shouldBe expectedObserveCommand.commandName()
        actualObserveCommand.maybeObsId() shouldBe expectedObserveCommand.maybeObsId()
    }

}
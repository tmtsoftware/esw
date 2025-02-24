package esw.ocs.dsl.highlevel

import csw.params.commands.CommandName
import csw.params.commands.Observe
import csw.params.commands.Setup
import csw.params.core.models.ObsId
import csw.prefix.models.Prefix
import esw.ocs.dsl.highlevel.models.ESW
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.*

@Suppress("DANGEROUS_CHARACTERS")
class CommandServiceDslTest : CommandServiceDsl {

    @Test
    fun `setup_method_should_construct_a_Setup_command_with_given_prefix,_commandName_and_obsId_|_ESW-121`(): Unit = runBlocking {
        val actualSetupCommand = Setup("esw.test", "move", "2020A-001-123")
        val expectedSetupCommand = Setup(Prefix(ESW, "test"), CommandName("move"), Optional.of(ObsId.apply("2020A-001-123")))

        actualSetupCommand.source() shouldBe expectedSetupCommand.source()
        actualSetupCommand.commandName() shouldBe expectedSetupCommand.commandName()
        actualSetupCommand.maybeObsId() shouldBe expectedSetupCommand.maybeObsId()
    }

    @Test
    fun `observe_method_should_construct_a_Observe_command_with_given_prefix,_commandName_and_obsId_|_ESW-121`(): Unit = runBlocking {
        val expectedObserveCommand = Observe(Prefix(ESW, "test"), CommandName("move"), Optional.of(ObsId.apply("2020A-001-123")))
        val actualObserveCommand = Observe("esw.test", "move", "2020A-001-123")
        actualObserveCommand.source() shouldBe expectedObserveCommand.source()
        actualObserveCommand.commandName() shouldBe expectedObserveCommand.commandName()
        actualObserveCommand.maybeObsId() shouldBe expectedObserveCommand.maybeObsId()
    }

}

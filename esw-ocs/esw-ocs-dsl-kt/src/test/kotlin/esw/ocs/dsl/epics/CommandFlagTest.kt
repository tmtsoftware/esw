package esw.ocs.dsl.epics

import esw.ocs.dsl.highlevel.CommandServiceDsl
import esw.ocs.dsl.params.Params
import esw.ocs.dsl.params.intKey
import esw.ocs.dsl.params.kMadd
import esw.ocs.dsl.params.set
import io.kotlintest.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class CommandFlagTest : CommandServiceDsl {
    @Test
    fun `set should update commandFlag value | ESW-142`() = runBlocking {
        val setup = setup("tcs", "command-1", "obsId")
                .kMadd(intKey("encoder").set(1))
        val setupCommandParams = setup.jParamSet()

        val commandFlag = CommandFlag()

        commandFlag.value() shouldBe Params(setOf())

        commandFlag.set(setupCommandParams)

        commandFlag.value() shouldBe Params(setupCommandParams)
    }
}

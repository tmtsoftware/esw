package esw.ocs.dsl.epics

import csw.params.core.generics.Parameter
import esw.ocs.dsl.highlevel.CommandServiceDsl
import esw.ocs.dsl.params.Params
import esw.ocs.dsl.params.intKey
import esw.ocs.dsl.params.kMadd
import esw.ocs.dsl.params.set
import io.kotlintest.eventually
import io.kotlintest.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import io.kotlintest.milliseconds as jMilliseconds

class CommandFlagTest : CommandServiceDsl {
    val timeout = 100.jMilliseconds

    @Test
    fun `set should update commandFlag value | ESW-252`() = runBlocking {
        val setup = setup("tcs", "command-1", "obsId")
                .kMadd(intKey("encoder").set(1))
        val setupCommandParams = setup.jParamSet()

        val commandFlag = CommandFlag()

        commandFlag.value() shouldBe Params(setOf())

        commandFlag.set(setupCommandParams)

        commandFlag.value() shouldBe Params(setupCommandParams)
    }

    @Test
    fun `bind should update subscribers of commandFlag | ESW-252`() = runBlocking {
        val flag = CommandFlag()
        val refreshable = mockk<Refreshable>()
        val params = mockk<Set<Parameter<*>>>()

        flag.bind(refreshable)
        every { refreshable.refresh() }.answers {}

        flag.set(params)

        eventually(timeout) {
            verify { refreshable.refresh() }
        }
    }
}

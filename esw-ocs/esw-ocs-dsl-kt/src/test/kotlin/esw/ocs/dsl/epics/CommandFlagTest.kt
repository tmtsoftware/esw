package esw.ocs.dsl.epics

import esw.ocs.dsl.highlevel.CommandServiceDsl
import esw.ocs.dsl.params.Params
import esw.ocs.dsl.params.intKey
import esw.ocs.dsl.params.params
import io.kotest.assertions.timing.eventually
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.time.milliseconds

class CommandFlagTest : CommandServiceDsl {
    private val timeout = 100.milliseconds

    @Test
    fun `set should update commandFlag value | ESW-252`() = runBlocking {
        val setup = Setup("TCS.test", "command-1", "2020A-P001-O123").madd(intKey("encoder").set(1))

        val commandFlag = CommandFlag()
        commandFlag.value() shouldBe Params(setOf())
        commandFlag.set(setup.params)
        commandFlag.value() shouldBe setup.params
    }

    @Test
    fun `bind should update subscribers of commandFlag | ESW-252`() = runBlocking {
        val flag = CommandFlag()
        val refreshable = mockk<Refreshable>()
        val params = mockk<Params>()

        flag.bind(refreshable)
        coEvery { refreshable.refresh() }.answers {}

        flag.set(params)

        eventually(timeout) {
            coVerify { refreshable.refresh() }
        }
    }
}

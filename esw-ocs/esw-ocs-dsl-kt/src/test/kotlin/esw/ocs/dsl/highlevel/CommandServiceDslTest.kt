package esw.ocs.dsl.highlevel

import csw.location.api.javadsl.JComponentType
import csw.location.models.ComponentType
import csw.params.commands.CommandName
import csw.params.commands.CommandResponse.*
import csw.params.commands.Observe
import csw.params.commands.Setup
import csw.params.core.models.Id
import csw.params.core.models.ObsId
import csw.params.core.models.Prefix
import csw.time.core.models.UTCTime
import esw.ocs.api.protocol.`Ok$`
import io.kotlintest.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*

class CommandServiceDslTest : CommandServiceDsl {

    private val hcdName = "sampleHcd"
    private val assemblyName = "sampleAssembly"

    private val setupCommand = setup("esw.test", "move", "testObsId")
    private val assemblyCommandService: RichComponent = mockk()
    private val hcdCommandService: RichComponent = mockk()
    private val sequencerCommandService: RichSequencer = mockk()

    override fun resolveComponent(name: String, componentType: ComponentType): RichComponent = when (componentType) {
        JComponentType.HCD() -> hcdCommandService
        JComponentType.Assembly() -> assemblyCommandService
        else -> throw IllegalArgumentException("Unsupported component type: $componentType provided!")
    }

    override fun resolveSequencer(sequencerId: String, observingMode: String): RichSequencer = sequencerCommandService

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

    @Nested
    inner class HCD {
        @Test
        fun `oneway should resolve RichComponent for given hcd and call oneway method on it | ESW-121`() = runBlocking {
            coEvery { hcdCommandService.oneway(setupCommand) }.answers { Accepted(Id.apply()) }

            val hcd = HCD(hcdName)
            hcd.oneway(setupCommand)

            coVerify { hcdCommandService.oneway(setupCommand) }
        }

        @Test
        fun `submitAndWait should resolve RichComponent for given hcd and call submitAndWait method on it | ESW-121`() = runBlocking {
            coEvery { hcdCommandService.submitAndWait(setupCommand) }.answers { Completed(Id.apply()) }

            val hcd = HCD(hcdName)
            hcd.submitAndWait(setupCommand)

            coVerify { hcdCommandService.submitAndWait(setupCommand) }
        }

        @Test
        fun `submit should resolve RichComponent for given hcd and call submit method on it | ESW-121`() = runBlocking {
            coEvery { hcdCommandService.submit(setupCommand) }.answers { Started(Id.apply()) }

            val hcd = HCD(hcdName)
            hcd.submit(setupCommand)

            coVerify { hcdCommandService.submit(setupCommand) }
        }

        @Test
        fun `validate should resolve RichComponent for given hcd and call validate method on it | ESW-121`() = runBlocking {
            coEvery { hcdCommandService.validate(setupCommand) }.answers { Accepted(Id.apply()) }

            val hcd = HCD(hcdName)
            hcd.validate(setupCommand)

            coVerify { hcdCommandService.validate(setupCommand) }
        }

        @Test
        fun `diagnosticMode should resolve RichComponent for given hcd and call diagnosticMode method on it | ESW-118`() = runBlocking {
            val hint = "test-hint"
            val startTime = UTCTime.now()
            coEvery { hcdCommandService.diagnosticMode(startTime, hint) }.answers { Unit }

            var hcd: RichComponent = HCD(hcdName)
            hcd.diagnosticMode(startTime, hint)
            coVerify { hcdCommandService.diagnosticMode(startTime, hint) }
        }

        @Test
        fun `operationsMode should resolve RichComponent for given hcd and call operationsMode method on it | ESW-118`() = runBlocking {
            coEvery { hcdCommandService.operationsMode() }.answers { Unit }

            val hcd = HCD(hcdName)
            hcd.operationsMode()

            coVerify { hcdCommandService.operationsMode() }
        }

        @Test
        fun `goOffline should resolve RichComponent for given hcd and call goOffline method on it | ESW-236`() = runBlocking {
            coEvery { hcdCommandService.goOffline() }.answers { Unit }

            val hcd = HCD(hcdName)
            hcd.goOffline()

            coVerify { hcdCommandService.goOffline() }
        }

        @Test
        fun `goOnline should resolve RichComponent for given hcd and call goOnline method on it | ESW-236`() = runBlocking {
            coEvery { hcdCommandService.goOnline() }.answers { Unit }

            val hcd = HCD(hcdName)
            hcd.goOnline()

            coVerify { hcdCommandService.goOnline() }
        }
    }

    @Nested
    inner class Assembly {
        @Test
        fun `validate should resolve RichComponent for given assembly and call validate method on it | ESW-121`() = runBlocking {
            coEvery { assemblyCommandService.validate(setupCommand) }.answers { Accepted(Id.apply()) }

            val assembly = Assembly(assemblyName)
            assembly.validate(setupCommand)

            coVerify { assemblyCommandService.validate(setupCommand) }
        }

        @Test
        fun `submit should resolve RichComponent for given assembly and call submit method on it | ESW-121`() = runBlocking {
            coEvery { assemblyCommandService.submit(setupCommand) }.answers { Started(Id.apply()) }

            val assembly = Assembly(assemblyName)
            assembly.submit(setupCommand)

            coVerify { assemblyCommandService.submit(setupCommand) }
        }

        @Test
        fun `submitAndWait should resolve RichComponent for given assembly and call submitAndWait method on it | ESW-121`() = runBlocking {
            coEvery { assemblyCommandService.submitAndWait(setupCommand) }.answers { Completed(Id.apply()) }

            val assembly = Assembly(assemblyName)
            assembly.submitAndWait(setupCommand)

            coVerify { assemblyCommandService.submitAndWait(setupCommand) }
        }

        @Test
        fun `oneway should resolve RichComponent for given assembly and call oneway method on it | ESW-121`() = runBlocking {
            coEvery { assemblyCommandService.oneway(setupCommand) }.answers { Accepted(Id.apply()) }

            val assembly = Assembly(assemblyName)
            assembly.oneway(setupCommand)

            coVerify { assemblyCommandService.oneway(setupCommand) }
        }

        @Test
        fun `diagnosticMode should resolve RichComponent for given assembly and call diagnosticMode method on it | ESW-118`() = runBlocking {
            val hint = "test-hint"
            val startTime: UTCTime = UTCTime.now()

            coEvery { assemblyCommandService.diagnosticMode(startTime, hint) }.answers { Unit }

            val assembly = Assembly(assemblyName)
            assembly.diagnosticMode(startTime, hint)

            coVerify { assemblyCommandService.diagnosticMode(startTime, hint) }
        }

        @Test
        fun `operationsMode should resolve RichComponent for given assembly and call operationsMode method on it | ESW-118`() = runBlocking {
            coEvery { assemblyCommandService.operationsMode() }.answers { Unit }

            val assembly = Assembly(assemblyName)
            assembly.operationsMode()

            coVerify { assemblyCommandService.operationsMode() }
        }

        @Test
        fun `goOffline should resolve RichComponent for given assembly and call goOffline method on it | ESW-236`() = runBlocking {
            coEvery { assemblyCommandService.goOffline() }.answers { Unit }

            val assembly = Assembly(assemblyName)
            assembly.goOffline()

            coVerify { assemblyCommandService.goOffline() }
        }

        @Test
        fun `goOnline should resolve RichComponent for given assembly and call goOnline method on it | ESW-236`() = runBlocking {
            coEvery { assemblyCommandService.goOnline() }.answers { Unit }

            val assembly = Assembly(assemblyName)
            assembly.goOnline()

            coVerify { assemblyCommandService.goOnline() }
        }
    }

    @Nested
    inner class Sequencer {
        @Test
        fun `goOnline should resolve RichSequencer for given sequencer and call goOnline method on it | ESW-236`() = runBlocking {
            val sequencerId = "testSequencer"
            val observingMode = "DarkNight"

            coEvery { sequencerCommandService.goOnline() }.answers { `Ok$`.`MODULE$` }

            val sequencer = Sequencer(sequencerId, observingMode)
            sequencer.goOnline()

            coVerify { sequencerCommandService.goOnline() }
        }

        @Test
        fun `goOffline should resolve RichSequencer for given sequencer and call goOffline method on it | ESW-236`() = runBlocking {
            val sequencerId = "testSequencer"
            val observingMode = "DarkNight"

            coEvery { sequencerCommandService.goOffline() }.answers { `Ok$`.`MODULE$` }

            val sequencer = Sequencer(sequencerId, observingMode)
            sequencer.goOffline()

            coVerify { sequencerCommandService.goOffline() }
        }

        @Test
        fun `abortSequence should resolve RichSequencer for given sequencer and call abortSequence method on it  | ESW-155, ESW-137`() = runBlocking {
            val sequencerId = "testSequencer"
            val observingMode = "DarkNight"

            coEvery { sequencerCommandService.abortSequence() }.answers { `Ok$`.`MODULE$` }

            val sequencer = Sequencer(sequencerId, observingMode)
            sequencer.abortSequence()

            coVerify { sequencerCommandService.abortSequence() }
        }

        @Test
        fun `diagnosticMode should resolve RichSequencer for given sequencer and call diagnosticMode method on it | ESW-143`() = runBlocking {
            val hint = "test-hint"
            val startTime: UTCTime = UTCTime.now()

            val sequencerId = "testSequencer"
            val observingMode = "DarkNight"

            coEvery { sequencerCommandService.diagnosticMode(startTime, hint) }.answers { `Ok$`.`MODULE$` }

            val sequencer = Sequencer(sequencerId, observingMode)
            sequencer.diagnosticMode(startTime, hint)

            coVerify { sequencerCommandService.diagnosticMode(startTime, hint) }
        }

        @Test
        fun `operationsMode should resolve RichSequencer for given sequencer and call operationsMode method on it | ESW-143`() = runBlocking {
            val sequencerId = "testSequencer"
            val observingMode = "DarkNight"

            coEvery { sequencerCommandService.operationsMode() }.answers { `Ok$`.`MODULE$` }

            val sequencer = Sequencer(sequencerId, observingMode)
            sequencer.operationsMode()

            coVerify { sequencerCommandService.operationsMode() }
        }

        @Test
        fun `stop should resolve RichSequencer for given sequencer and call stop method on it | ESW-156, ESW-138`() = runBlocking {
            val sequencerId = "testSequencer"
            val observingMode = "DarkNight"

            coEvery { sequencerCommandService.stop() }.answers { `Ok$`.`MODULE$` }

            val sequencer = Sequencer(sequencerId, observingMode)
            sequencer.stop()

            coVerify { sequencerCommandService.stop() }
        }
    }

}
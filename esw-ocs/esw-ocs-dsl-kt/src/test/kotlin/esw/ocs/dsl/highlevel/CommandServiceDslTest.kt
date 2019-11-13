package esw.ocs.dsl.highlevel

import csw.params.commands.CommandName
import csw.params.commands.CommandResponse.*
import csw.params.commands.Observe
import csw.params.commands.Setup
import csw.params.core.models.Id
import csw.params.core.models.ObsId
import csw.params.core.models.Prefix
import csw.time.core.models.UTCTime
import esw.ocs.api.protocol.`Ok$`
import esw.ocs.dsl.highlevel.internal.InternalCommandService
import esw.ocs.dsl.highlevel.internal.InternalSequencerCommandService
import io.kotlintest.shouldBe
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.*

class CommandServiceDslTest : CommandServiceDsl {
    override val commonUtils: CommonUtils = mockk()

    private val hcdName = "sampleHcd"
    private val assemblyName = "sampleAssembly"

    private val setupCommand = setup("esw.test", "move", "testObsId")
    private val assemblyCommandService: InternalCommandService = mockk()
    private val hcdCommandService: InternalCommandService = mockk()
    private val sequencerCommandService: InternalSequencerCommandService = mockk()

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

    @Test
    fun `HCD()#validate should resolve InternalCommandService for given hcd and call validate method on it | ESW-121`() = runBlocking {

        coEvery { commonUtils.resolveHcd(hcdName) }.answers { hcdCommandService }
        coEvery { hcdCommandService.validate(setupCommand) }.answers { Accepted(Id.apply()) }

        val hcd = HCD(hcdName)
        hcd.validate(setupCommand)

        coVerify { hcdCommandService.validate(setupCommand) }
    }

    @Test
    fun `Assembly()#validate should resolve InternalCommandService for given assembly and call validate method on it | ESW-121`() = runBlocking {

        coEvery { commonUtils.resolveAssembly(assemblyName) }.answers { assemblyCommandService }
        coEvery { assemblyCommandService.validate(setupCommand) }.answers { Accepted(Id.apply()) }

        val assembly = Assembly(assemblyName)
        assembly.validate(setupCommand)

        coVerify { assemblyCommandService.validate(setupCommand) }
    }

    @Test
    fun `Hcd#submit should resolve InternalCommandService for given hcd and call submit method on it | ESW-121`() = runBlocking {

        coEvery { commonUtils.resolveHcd(hcdName) }.answers { hcdCommandService }
        coEvery { hcdCommandService.submit(setupCommand) }.answers { Started(Id.apply()) }

        val hcd = HCD(hcdName)
        hcd.submit(setupCommand)

        coVerify { hcdCommandService.submit(setupCommand) }
    }

    @Test
    fun `Assembly()#submit should resolve InternalCommandService for given assembly and call submit method on it | ESW-121`() = runBlocking {

        coEvery { commonUtils.resolveAssembly(assemblyName) }.answers { assemblyCommandService }
        coEvery { assemblyCommandService.submit(setupCommand) }.answers { Started(Id.apply()) }

        val assembly = Assembly(assemblyName)
        assembly.submit(setupCommand)

        coVerify { assemblyCommandService.submit(setupCommand) }
    }

    @Test
    fun `Hcd()#submitAndWait should resolve InternalCommandService for given hcd and call submitAndWait method on it | ESW-121`() = runBlocking {

        coEvery { commonUtils.resolveHcd(hcdName) }.answers { hcdCommandService }
        coEvery { hcdCommandService.submitAndWait(setupCommand) }.answers { Completed(Id.apply()) }

        val hcd = HCD(hcdName)
        hcd.submitAndWait(setupCommand)

        coVerify { hcdCommandService.submitAndWait(setupCommand) }
    }

    @Test
    fun `Assembly()#submitAndWait should resolve InternalCommandService for given assembly and call submitAndWait method on it | ESW-121`() = runBlocking {

        coEvery { commonUtils.resolveAssembly(assemblyName) }.answers { assemblyCommandService }
        coEvery { assemblyCommandService.submitAndWait(setupCommand) }.answers { Completed(Id.apply()) }

        val assembly = Assembly(assemblyName)
        assembly.submitAndWait(setupCommand)

        coVerify { assemblyCommandService.submitAndWait(setupCommand) }
    }

    @Test
    fun `Hcd#oneway should resolve InternalCommandService for given hcd and call oneway method on it | ESW-121`() = runBlocking {

        coEvery { commonUtils.resolveHcd(hcdName) }.answers { hcdCommandService }
        coEvery { hcdCommandService.oneway(setupCommand) }.answers { Accepted(Id.apply()) }

        val hcd = HCD(hcdName)
        hcd.oneway(setupCommand)

        coVerify { hcdCommandService.oneway(setupCommand) }
    }

    @Test
    fun `Assembly()#oneway should resolve InternalCommandService for given assembly and call oneway method on it | ESW-121`() = runBlocking {

        coEvery { commonUtils.resolveAssembly(assemblyName) }.answers { assemblyCommandService }
        coEvery { assemblyCommandService.oneway(setupCommand) }.answers { Accepted(Id.apply()) }

        val assembly = Assembly(assemblyName)
        assembly.oneway(setupCommand)

        coVerify { assemblyCommandService.oneway(setupCommand) }
    }

    @Test
    fun `Assembly()#diagnosticMode should resolve InternalCommandService for given assembly and call diagnosticMode method on it | ESW-118`() = runBlocking {
        val hint = "test-hint"
        val startTime: UTCTime = UTCTime.now()

        coEvery { commonUtils.resolveAssembly(assemblyName) }.answers { assemblyCommandService }
        coEvery { assemblyCommandService.diagnosticMode(startTime, hint) }.answers { Unit }

        val assembly = Assembly(assemblyName)
        assembly.diagnosticMode(startTime, hint)

        coVerify { assemblyCommandService.diagnosticMode(startTime, hint) }
    }

    @Test
    fun `Assembly()#operationsMode should resolve InternalCommandService for given assembly and call operationsMode method on it | ESW-118`() = runBlocking {

        coEvery { commonUtils.resolveAssembly(assemblyName) }.answers { assemblyCommandService }
        coEvery { assemblyCommandService.operationsMode() }.answers { Unit }

        val assembly = Assembly(assemblyName)
        assembly.operationsMode()

        coVerify { assemblyCommandService.operationsMode() }
    }

    @Test
    fun `HCD()#diagnosticMode should resolve InternalCommandService for given hcd and call diagnosticMode method on it | ESW-118`() = runBlocking {
        val hint = "test-hint"
        val startTime: UTCTime = UTCTime.now()

        coEvery { commonUtils.resolveHcd(hcdName) }.answers { hcdCommandService }
        coEvery { hcdCommandService.diagnosticMode(startTime, hint) }.answers { Unit }

        val hcd = HCD(hcdName)
        hcd.diagnosticMode(startTime, hint)

        coVerify { hcdCommandService.diagnosticMode(startTime, hint) }
    }

    @Test
    fun `HCD()#operationsMode should resolve InternalCommandService for given hcd and call operationsMode method on it | ESW-118`() = runBlocking {

        coEvery { commonUtils.resolveHcd(hcdName) }.answers { hcdCommandService }
        coEvery { hcdCommandService.operationsMode() }.answers { Unit }

        val hcd = HCD(hcdName)
        hcd.operationsMode()

        coVerify { hcdCommandService.operationsMode() }
    }

    @Test
    fun `Assembly()#goOffline should resolve InternalCommandService for given assembly and call goOffline method on it | ESW-118`() = runBlocking {

        coEvery { commonUtils.resolveAssembly(assemblyName) }.answers { assemblyCommandService }
        coEvery { assemblyCommandService.goOffline() }.answers { Unit }

        val assembly = Assembly(assemblyName)
        assembly.goOffline()

        coVerify { assemblyCommandService.goOffline() }
    }

    @Test
    fun `Assembly()#goOnline should resolve InternalCommandService for given assembly and call goOnline method on it | ESW-118`() = runBlocking {

        coEvery { commonUtils.resolveAssembly(assemblyName) }.answers { assemblyCommandService }
        coEvery { assemblyCommandService.goOnline() }.answers { Unit }

        val assembly = Assembly(assemblyName)
        assembly.goOnline()

        coVerify { assemblyCommandService.goOnline() }
    }

    @Test
    fun `HCD()#goOffline should resolve InternalCommandService for given hcd and call goOffline method on it | ESW-118`() = runBlocking {

        coEvery { commonUtils.resolveHcd(hcdName) }.answers { hcdCommandService }
        coEvery { hcdCommandService.goOffline() }.answers { Unit }

        val hcd = HCD(hcdName)
        hcd.goOffline()

        coVerify { hcdCommandService.goOffline() }
    }

    @Test
    fun `HCD()#goOnline should resolve InternalCommandService for given hcd and call goOnline method on it | ESW-118`() = runBlocking {

        coEvery { commonUtils.resolveHcd(hcdName) }.answers { hcdCommandService }
        coEvery { hcdCommandService.goOnline() }.answers { Unit }

        val hcd = HCD(hcdName)
        hcd.goOnline()

        coVerify { hcdCommandService.goOnline() }
    }

    @Test
    fun `Sequencer()#goOnline should resolve InternalSequencerCommandService for given sequencer and call goOnline method on it | ESW-236`() = runBlocking {

        val sequencerId = "testSequencer"
        val observingMode = "DarkNight"

        coEvery { commonUtils.resolveSequencer(sequencerId, observingMode) }.answers { sequencerCommandService }
        coEvery{sequencerCommandService.goOnline()}.answers{`Ok$`.`MODULE$`}

        val sequencer = Sequencer(sequencerId, observingMode)
        sequencer.goOnline()

        coVerify { sequencerCommandService.goOnline() }
    }

    @Test
    fun `Sequencer()#goOffline should resolve InternalSequencerCommandService for given sequencer and call goOffline method on it | ESW-236`() = runBlocking {

        val sequencerId = "testSequencer"
        val observingMode = "DarkNight"

        coEvery { commonUtils.resolveSequencer(sequencerId, observingMode) }.answers { sequencerCommandService }
        coEvery{sequencerCommandService.goOffline()}.answers{`Ok$`.`MODULE$`}

        val sequencer = Sequencer(sequencerId, observingMode)
        sequencer.goOffline()

        coVerify { sequencerCommandService.goOffline() }
    }

    @Test
    fun `Sequencer()#abortSequence should resolve InternalSequencerCommandService for given sequencer and call abortSequence method on it  | ESW-155, ESW-137`() = runBlocking {

        val sequencerId = "testSequencer"
        val observingMode = "DarkNight"

        coEvery { commonUtils.resolveSequencer(sequencerId, observingMode) }.answers { sequencerCommandService }
        coEvery{sequencerCommandService.abortSequence()}.answers{`Ok$`.`MODULE$`}

        val sequencer = Sequencer(sequencerId, observingMode)
        sequencer.abortSequence()

        coVerify { sequencerCommandService.abortSequence() }
    }

    @Test
    fun `Sequencer()#diagnosticMode should resolve InternalSequencerCommandService for given sequencer and call diagnosticMode method on it | ESW-143`() = runBlocking {
        val hint = "test-hint"
        val startTime: UTCTime = UTCTime.now()

        val sequencerId = "testSequencer"
        val observingMode = "DarkNight"

        coEvery { commonUtils.resolveSequencer(sequencerId, observingMode) }.answers { sequencerCommandService }
        coEvery{sequencerCommandService.diagnosticMode(startTime, hint)}.answers{`Ok$`.`MODULE$`}

        val sequencer = Sequencer(sequencerId, observingMode)
        sequencer.diagnosticMode(startTime, hint)

        coVerify { sequencerCommandService.diagnosticMode(startTime, hint) }
    }

    @Test
    fun `Sequencer()#operationsMode should resolve InternalSequencerCommandService for given sequencer and call operationsMode method on it | ESW-143`() = runBlocking {

        val sequencerId = "testSequencer"
        val observingMode = "DarkNight"

        coEvery { commonUtils.resolveSequencer(sequencerId, observingMode) }.answers { sequencerCommandService }
        coEvery{sequencerCommandService.operationsMode()}.answers{`Ok$`.`MODULE$`}

        val sequencer = Sequencer(sequencerId, observingMode)
        sequencer.operationsMode()

        coVerify { sequencerCommandService.operationsMode() }
    }

    @Test
    fun `Sequencer()#stop should resolve InternalSequencerCommandService for given sequencer and call stop method on it | ESW-156, ESW-138`() = runBlocking {

        val sequencerId = "testSequencer"
        val observingMode = "DarkNight"

        coEvery { commonUtils.resolveSequencer(sequencerId, observingMode) }.answers { sequencerCommandService }
        coEvery{sequencerCommandService.stop()}.answers{`Ok$`.`MODULE$`}

        val sequencer = Sequencer(sequencerId, observingMode)
        sequencer.stop()

        coVerify { sequencerCommandService.stop() }
    }

}
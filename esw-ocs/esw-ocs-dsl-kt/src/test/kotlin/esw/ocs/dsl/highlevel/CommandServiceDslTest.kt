package esw.ocs.dsl.highlevel

import akka.actor.typed.ActorSystem
import csw.command.api.javadsl.ICommandService
import csw.command.client.CommandServiceFactory
import csw.location.api.javadsl.ILocationService
import csw.location.api.javadsl.JComponentType
import csw.location.models.AkkaLocation
import csw.location.models.ComponentId
import csw.location.models.Connection
import csw.params.commands.CommandName
import csw.params.commands.CommandResponse.*
import csw.params.commands.Observe
import csw.params.commands.Setup
import csw.params.core.models.ObsId
import csw.params.core.models.Prefix
import io.kotlintest.shouldBe
import io.kotlintest.specs.AbstractAnnotationSpec
import io.kotlintest.specs.AbstractAnnotationSpec.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.CompletableFuture

class CommandServiceDslTest : CommandServiceDsl {
    override val locationService: ILocationService = mockk()
    override val actorSystem: ActorSystem<*> = mockk()
    private val akkaLocation: AkkaLocation = mockk()
    private val commandService: ICommandService = mockk()

    private val hcdName = "sampleHcd"
    private val assemblyName = "sampleAssembly"

    private val hcdAkkaConnection = Connection.AkkaConnection(ComponentId(hcdName, JComponentType.HCD()))
    private val assemblyAkkaConnection = Connection.AkkaConnection(ComponentId(assemblyName, JComponentType.Assembly()))
    private val setupCommand = setup("esw.test", "move", "testObsId")

    @BeforeEach
    fun setup() {
        mockkStatic(CommandServiceFactory::class)
        every { CommandServiceFactory.jMake(akkaLocation, actorSystem) }.answers { commandService }
    }

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
    fun `HCD should resolve commandService for given hcd and call validate method on it | ESW-121`() = runBlocking {

        mockkStatic(CommandServiceFactory::class)
        every { CommandServiceFactory.jMake(akkaLocation, actorSystem) }.answers { commandService }
        every { locationService.resolve(hcdAkkaConnection, any()) }.answers { CompletableFuture.completedFuture(Optional.of(akkaLocation)) }
        every { commandService.validate(setupCommand) }.answers { CompletableFuture.completedFuture(Accepted(setupCommand.runId())) }

        val hcd = HCD(hcdName)
        hcd.validate(setupCommand)

        verify { commandService.validate(setupCommand) }
    }

    @Test
    fun `Assembly should resolve commandService for given assembly and call validate method on it | ESW-121`() = runBlocking {

        mockkStatic(CommandServiceFactory::class)
        every { CommandServiceFactory.jMake(akkaLocation, actorSystem) }.answers { commandService }
        every { locationService.resolve(assemblyAkkaConnection, any()) }.answers { CompletableFuture.completedFuture(Optional.of(akkaLocation)) }
        every { commandService.validate(setupCommand) }.answers { CompletableFuture.completedFuture(Accepted(setupCommand.runId())) }

        val assembly = Assembly(assemblyName)
        assembly.validate(setupCommand)

        verify { commandService.validate(setupCommand) }
    }

    @Test
    fun `Hcd should resolve commandService for given hcd and call submit method on it | ESW-121`() = runBlocking {

        mockkStatic(CommandServiceFactory::class)
        every { CommandServiceFactory.jMake(akkaLocation, actorSystem) }.answers { commandService }
        every { locationService.resolve(hcdAkkaConnection, any()) }.answers { CompletableFuture.completedFuture(Optional.of(akkaLocation)) }
        every { commandService.submit(setupCommand, any()) }.answers { CompletableFuture.completedFuture(Started(setupCommand.runId())) }

        val hcd = HCD(hcdName)
        hcd.submit(setupCommand)

        verify { commandService.submit(setupCommand, any()) }
    }

    @Test
    fun `Assembly should resolve commandService for given assembly and call submit method on it | ESW-121`() = runBlocking {

        mockkStatic(CommandServiceFactory::class)
        every { CommandServiceFactory.jMake(akkaLocation, actorSystem) }.answers { commandService }
        every { locationService.resolve(assemblyAkkaConnection, any()) }.answers { CompletableFuture.completedFuture(Optional.of(akkaLocation)) }
        every { commandService.submit(setupCommand, any()) }.answers { CompletableFuture.completedFuture(Started(setupCommand.runId())) }

        val assembly = Assembly(assemblyName)
        assembly.submit(setupCommand)

        verify { commandService.submit(setupCommand, any()) }
    }

    @Test
    fun `Hcd should resolve commandService for given hcd and call submitAndWait method on it | ESW-121`() = runBlocking {

        mockkStatic(CommandServiceFactory::class)
        every { CommandServiceFactory.jMake(akkaLocation, actorSystem) }.answers { commandService }
        every { locationService.resolve(hcdAkkaConnection, any()) }.answers { CompletableFuture.completedFuture(Optional.of(akkaLocation)) }
        every { commandService.submitAndWait(setupCommand, any()) }.answers { CompletableFuture.completedFuture(Completed(setupCommand.runId())) }

        val hcd = HCD(hcdName)
        hcd.submitAndWait(setupCommand)

        verify { commandService.submitAndWait(setupCommand, any()) }
    }

    @Test
    fun `Assembly should resolve commandService for given assembly and call submitAndWait method on it | ESW-121`() = runBlocking {

        mockkStatic(CommandServiceFactory::class)
        every { CommandServiceFactory.jMake(akkaLocation, actorSystem) }.answers { commandService }
        every { locationService.resolve(assemblyAkkaConnection, any()) }.answers { CompletableFuture.completedFuture(Optional.of(akkaLocation)) }
        every { commandService.submitAndWait(setupCommand, any()) }.answers { CompletableFuture.completedFuture(Completed(setupCommand.runId())) }

        val assembly = Assembly(assemblyName)
        assembly.submitAndWait(setupCommand)

        verify { commandService.submitAndWait(setupCommand, any()) }
    }

    @Test
    fun `Hcd should resolve commandService for given hcd and call oneway method on it | ESW-121`() = runBlocking {

        mockkStatic(CommandServiceFactory::class)
        every { CommandServiceFactory.jMake(akkaLocation, actorSystem) }.answers { commandService }
        every { locationService.resolve(hcdAkkaConnection, any()) }.answers { CompletableFuture.completedFuture(Optional.of(akkaLocation)) }
        every { commandService.oneway(setupCommand, any()) }.answers { CompletableFuture.completedFuture(Accepted(setupCommand.runId())) }

        val hcd = HCD(hcdName)
        hcd.oneway(setupCommand)

        verify { commandService.oneway(setupCommand, any()) }
    }

    @Test
    fun `Assembly should resolve commandService for given assembly and call oneway method on it | ESW-121`() = runBlocking {

        mockkStatic(CommandServiceFactory::class)
        every { CommandServiceFactory.jMake(akkaLocation, actorSystem) }.answers { commandService }
        every { locationService.resolve(assemblyAkkaConnection, any()) }.answers { CompletableFuture.completedFuture(Optional.of(akkaLocation)) }
        every { commandService.oneway(setupCommand, any()) }.answers { CompletableFuture.completedFuture(Accepted(setupCommand.runId())) }

        val assembly = Assembly(assemblyName)
        assembly.oneway(setupCommand)

        verify { commandService.oneway(setupCommand, any()) }
    }
}
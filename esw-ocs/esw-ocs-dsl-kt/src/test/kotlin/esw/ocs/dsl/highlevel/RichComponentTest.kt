package esw.ocs.dsl.highlevel

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.util.Timeout
import csw.command.api.javadsl.ICommandService
import csw.command.client.CommandServiceFactory
import csw.command.client.messages.ComponentMessage
import csw.command.client.messages.DiagnosticDataMessage
import csw.command.client.messages.RunningMessage
import csw.command.client.models.framework.LockingResponse
import csw.command.client.models.framework.ToComponentLifecycleMessage
import csw.location.api.javadsl.JComponentType
import csw.location.models.AkkaLocation
import csw.location.models.ComponentType
import csw.params.commands.CommandName
import csw.params.commands.CommandResponse
import csw.params.commands.Setup
import csw.params.core.models.Id
import csw.params.core.models.ObsId
import csw.params.core.models.Prefix
import csw.time.core.models.UTCTime
import esw.ocs.dsl.script.utils.LockUnlockUtil
import esw.ocs.dsl.sequence_manager.LocationServiceUtil
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration
import kotlin.time.seconds
import kotlin.time.toJavaDuration

class RichComponentTest {
    private val hint = "test-hint"
    private val startTime: UTCTime = UTCTime.now()

    private val componentName: String = "tcsAssembly"
    private val componentType: ComponentType = JComponentType.Assembly()
    private val setupCommand = Setup(Prefix("esw.test"), CommandName("move"), Optional.of(ObsId("testObsId")))

    private val prefixStr = "esw"
    private val prefix = Prefix(prefixStr)
    private val leaseDuration: Duration = 10.seconds
    private val jLeaseDuration: java.time.Duration = leaseDuration.toJavaDuration()

    private val lockUnlockUtil: LockUnlockUtil = mockk()
    private val locationServiceUtil: LocationServiceUtil = mockk()
    private val actorSystem: ActorSystem<*> = mockk()
    private val timeout: Timeout = mockk()
    private val coroutineScope: CoroutineScope = mockk()

    private val assembly: RichComponent =
            RichComponent(
                    componentName,
                    componentType,
                    lockUnlockUtil,
                    locationServiceUtil,
                    actorSystem,
                    timeout,
                    coroutineScope
            )

    private val assemblyLocation: AkkaLocation = mockk()
    private val assemblyRef: ActorRef<ComponentMessage> = mockk()
    private val assemblyCommandService: ICommandService = mockk()

    @Test
    fun `validate should resolve commandService for given assembly and call validate method on it | ESW-121, ESW-245 `() = runBlocking {
        mockkStatic(CommandServiceFactory::class)
        every { locationServiceUtil.jResolveAkkaLocation(componentName, componentType) }.answers { CompletableFuture.completedFuture(assemblyLocation) }
        every { CommandServiceFactory.jMake(assemblyLocation, actorSystem) }.answers { assemblyCommandService }
        every { assemblyCommandService.validate(setupCommand) }.answers { CompletableFuture.completedFuture(CommandResponse.Accepted(Id.apply())) }

        assembly.validate(setupCommand)

        verify { assemblyCommandService.validate(setupCommand) }
    }

    @Test
    fun `oneway should resolve commandService for given assembly and call oneway method on it | ESW-121, ESW-245 `() = runBlocking {
        mockkStatic(CommandServiceFactory::class)
        every { locationServiceUtil.jResolveAkkaLocation(componentName, componentType) }.answers { CompletableFuture.completedFuture(assemblyLocation) }
        every { CommandServiceFactory.jMake(assemblyLocation, actorSystem) }.answers { assemblyCommandService }
        every { assemblyCommandService.oneway(setupCommand, any()) }.answers { CompletableFuture.completedFuture(CommandResponse.Accepted(Id.apply())) }

        assembly.oneway(setupCommand)

        verify { assemblyCommandService.oneway(setupCommand, any()) }
    }

    @Test
    fun `submit should resolve commandService for given assembly and call submit method on it | ESW-121, ESW-245 `() = runBlocking {
        mockkStatic(CommandServiceFactory::class)
        every { locationServiceUtil.jResolveAkkaLocation(componentName, componentType) }.answers { CompletableFuture.completedFuture(assemblyLocation) }
        every { CommandServiceFactory.jMake(assemblyLocation, actorSystem) }.answers { assemblyCommandService }
        every { assemblyCommandService.submit(setupCommand, any()) }.answers { CompletableFuture.completedFuture(CommandResponse.Completed(Id.apply())) }

        assembly.submit(setupCommand)

        verify { assemblyCommandService.submit(setupCommand, any()) }
    }

    @Test
    fun `submitAndWait should resolve commandService for given assembly and call submitAndWait method on it | ESW-121, ESW-245 `() = runBlocking {
        mockkStatic(CommandServiceFactory::class)
        every { locationServiceUtil.jResolveAkkaLocation(componentName, componentType) }.answers { CompletableFuture.completedFuture(assemblyLocation) }
        every { CommandServiceFactory.jMake(assemblyLocation, actorSystem) }.answers { assemblyCommandService }
        every { assemblyCommandService.submitAndWait(setupCommand, any()) }.answers { CompletableFuture.completedFuture(CommandResponse.Completed(Id.apply())) }

        assembly.submitAndWait(setupCommand)

        verify { assemblyCommandService.submitAndWait(setupCommand, any()) }
    }

    @Test
    fun `diagnosticMode should resolve actorRef for given assembly and send DiagnosticMode message to it | ESW-118, ESW-245 `() = runBlocking {
        val diagnosticMessage = DiagnosticDataMessage.DiagnosticMode(startTime, hint)

        every { locationServiceUtil.jResolveComponentRef(componentName, componentType) }.answers { CompletableFuture.completedFuture(assemblyRef) }
        every { assemblyRef.tell(diagnosticMessage) }.answers { Unit }

        assembly.diagnosticMode(startTime, hint)

        verify { assemblyRef.tell(diagnosticMessage) }
    }

    @Test
    fun `operationsMode should resolve actorRef for given assembly and send OperationsMode message to it | ESW-118, ESW-245 `() = runBlocking {
        val operationsModeMessage = DiagnosticDataMessage.`OperationsMode$`.`MODULE$`

        every { locationServiceUtil.jResolveComponentRef(componentName, componentType) }.answers { CompletableFuture.completedFuture(assemblyRef) }
        every { assemblyRef.tell(operationsModeMessage) }.answers { Unit }

        assembly.operationsMode()

        verify { assemblyRef.tell(operationsModeMessage) }
    }

    @Test
    fun `goOnline should resolve actorRef for given assembly and send GoOnline message to it | ESW-236, ESW-245 `() = runBlocking {
        val goOnlineMessage = RunningMessage.Lifecycle(ToComponentLifecycleMessage.`GoOnline$`.`MODULE$`)

        every { locationServiceUtil.jResolveComponentRef(componentName, componentType) }.answers { CompletableFuture.completedFuture(assemblyRef) }
        every { assemblyRef.tell(goOnlineMessage) }.answers { Unit }

        assembly.goOnline()

        verify { assemblyRef.tell(goOnlineMessage) }
    }

    @Test
    fun `goOffline should resolve actorRef for given assembly and send GoOffline message to it | ESW-236, ESW-245 `() = runBlocking {
        val goOfflineMessage = RunningMessage.Lifecycle(ToComponentLifecycleMessage.`GoOffline$`.`MODULE$`)

        every { locationServiceUtil.jResolveComponentRef(componentName, componentType) }.answers { CompletableFuture.completedFuture(assemblyRef) }
        every { assemblyRef.tell(goOfflineMessage) }.answers { Unit }

        assembly.goOffline()

        verify { assemblyRef.tell(goOfflineMessage) }
    }

    @Test
    fun `lock should resolve actorRef for given assembly and send Lock message to it | ESW-126, ESW-245 `() = runBlocking {
        every { locationServiceUtil.jResolveComponentRef(componentName, componentType) }.answers { CompletableFuture.completedFuture(assemblyRef) }
        every { lockUnlockUtil.lock(assemblyRef, prefix, jLeaseDuration, any(), any()) }.answers { CompletableFuture.completedFuture(LockingResponse.`LockAcquired$`.`MODULE$`) }

        assembly.lock(prefixStr, leaseDuration, {}, {})

        verify { lockUnlockUtil.lock(assemblyRef, prefix, jLeaseDuration, any(), any()) }
    }

    @Test
    fun `unlock should resolve actorRef for given assembly and send Unlock message to it | ESW-126, ESW-245 `() = runBlocking {
        every { locationServiceUtil.jResolveComponentRef(componentName, componentType) }.answers { CompletableFuture.completedFuture(assemblyRef) }
        every { lockUnlockUtil.unlock(assemblyRef, prefix) }.answers { CompletableFuture.completedFuture(LockingResponse.`LockReleased$`.`MODULE$`) }

        assembly.unlock(prefixStr)

        verify { lockUnlockUtil.unlock(assemblyRef, prefix) }
    }

}
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
import csw.params.core.states.StateName
import csw.prefix.javadsl.JSubsystem.ESW
import csw.prefix.models.Prefix
import csw.time.core.models.UTCTime
import esw.ocs.dsl.highlevel.models.CommandError
import esw.ocs.dsl.script.utils.LockUnlockUtil
import esw.ocs.impl.internal.LocationServiceUtil
import io.kotlintest.shouldNotThrow
import io.kotlintest.shouldThrow
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import msocket.api.Subscription
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.seconds
import kotlin.time.toJavaDuration

class RichComponentTest {
    private val coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)

    private val hint = "test-hint"
    private val startTime: UTCTime = UTCTime.now()

    private val source = Prefix(ESW(), "test")
    private val setupCommand = Setup(source, CommandName("move"), Optional.of(ObsId("testObsId")))

    private val leaseDuration: Duration = 10.seconds
    private val jLeaseDuration: java.time.Duration = leaseDuration.toJavaDuration()

    private val lockUnlockUtil: LockUnlockUtil = mockk()
    private val locationServiceUtil: LocationServiceUtil = mockk()
    private val actorSystem: ActorSystem<*> = mockk()

    private val timeoutDuration: Duration = 5.seconds
    private val timeout = Timeout(timeoutDuration.toLongNanoseconds(), TimeUnit.NANOSECONDS)

    private val defaultTimeoutDuration: Duration = 5.seconds
    private val defaultTimeout = Timeout(defaultTimeoutDuration.toLongNanoseconds(), TimeUnit.NANOSECONDS)

    @Nested
    inner class Assembly {
        private val componentName: String = "sampleAssembly"
        private val componentType: ComponentType = JComponentType.Assembly()
        private val prefix = Prefix(ESW(), componentName)
        private val assembly: RichComponent =
                RichComponent(
                        prefix,
                        componentType,
                        lockUnlockUtil,
                        locationServiceUtil,
                        actorSystem,
                        defaultTimeoutDuration,
                        coroutineScope
                )

        private val assemblyLocation: AkkaLocation = mockk()
        private val assemblyRef: ActorRef<ComponentMessage> = mockk()
        private val assemblyCommandService: ICommandService = mockk()

        @Test
        fun `validate should resolve commandService for given assembly and call validate method on it | ESW-121, ESW-245 `() = runBlocking {
            mockkStatic(CommandServiceFactory::class)
            every { locationServiceUtil.jResolveAkkaLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyLocation) }
            every { CommandServiceFactory.jMake(assemblyLocation, actorSystem) }.answers { assemblyCommandService }
            every { assemblyCommandService.validate(setupCommand) }.answers { CompletableFuture.completedFuture(CommandResponse.Accepted(Id.apply())) }

            assembly.validate(setupCommand)

            verify { assemblyCommandService.validate(setupCommand) }
        }

        @Test
        fun `oneway should resolve commandService for given assembly and call oneway method on it | ESW-121, ESW-245 `() = runBlocking {
            mockkStatic(CommandServiceFactory::class)
            every { locationServiceUtil.jResolveAkkaLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyLocation) }
            every { CommandServiceFactory.jMake(assemblyLocation, actorSystem) }.answers { assemblyCommandService }
            every { assemblyCommandService.oneway(setupCommand) }.answers { CompletableFuture.completedFuture(CommandResponse.Accepted(Id.apply())) }

            assembly.oneway(setupCommand)

            verify { assemblyCommandService.oneway(setupCommand) }
        }

        @Test
        fun `submit should resolve commandService for given assembly and call submit method on it | ESW-121, ESW-245 `() = runBlocking {
            mockkStatic(CommandServiceFactory::class)
            every { locationServiceUtil.jResolveAkkaLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyLocation) }
            every { CommandServiceFactory.jMake(assemblyLocation, actorSystem) }.answers { assemblyCommandService }
            every { assemblyCommandService.submit(setupCommand) }.answers { CompletableFuture.completedFuture(CommandResponse.Completed(Id.apply())) }

            assembly.submit(setupCommand)

            verify { assemblyCommandService.submit(setupCommand) }
        }

        @Test
        fun `query should resolve commandService for given assembly and call query method on it | ESW-121, ESW-245 `() = runBlocking {
            val commandRunId: Id = mockk()

            mockkStatic(CommandServiceFactory::class)
            every { locationServiceUtil.jResolveAkkaLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyLocation) }
            every { CommandServiceFactory.jMake(assemblyLocation, actorSystem) }.answers { assemblyCommandService }
            every { assemblyCommandService.query(commandRunId) }.answers { CompletableFuture.completedFuture(CommandResponse.Completed(Id.apply())) }

            assembly.query(commandRunId)

            verify { assemblyCommandService.query(commandRunId) }
        }

        @Test
        fun `queryFinal should resolve commandService for given assembly and call queryFinal method on it | ESW-121, ESW-245 `() = runBlocking {
            val commandRunId: Id = mockk()

            mockkStatic(CommandServiceFactory::class)
            every { locationServiceUtil.jResolveAkkaLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyLocation) }
            every { CommandServiceFactory.jMake(assemblyLocation, actorSystem) }.answers { assemblyCommandService }
            every { assemblyCommandService.queryFinal(commandRunId, timeout) }.answers { CompletableFuture.completedFuture(CommandResponse.Completed(Id.apply())) }

            assembly.queryFinal(commandRunId, timeoutDuration)

            verify { assemblyCommandService.queryFinal(commandRunId, timeout) }
        }

        @Test
        fun `queryFinal should resolve commandService for given assembly and call queryFinal method on it with defaultTimeout if timeout is not provided | ESW-121, ESW-245 `() = runBlocking {
            val commandRunId: Id = mockk()

            mockkStatic(CommandServiceFactory::class)
            every { locationServiceUtil.jResolveAkkaLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyLocation) }
            every { CommandServiceFactory.jMake(assemblyLocation, actorSystem) }.answers { assemblyCommandService }
            every { assemblyCommandService.queryFinal(commandRunId, defaultTimeout) }.answers { CompletableFuture.completedFuture(CommandResponse.Completed(Id.apply())) }

            assembly.queryFinal(commandRunId)

            verify { assemblyCommandService.queryFinal(commandRunId, defaultTimeout) }
        }

        @Test
        fun `submitAndWait should resolve commandService for given assembly and call submitAndWait method on it | ESW-121, ESW-245 `() = runBlocking {
            mockkStatic(CommandServiceFactory::class)
            every { locationServiceUtil.jResolveAkkaLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyLocation) }
            every { CommandServiceFactory.jMake(assemblyLocation, actorSystem) }.answers { assemblyCommandService }
            every { assemblyCommandService.submitAndWait(setupCommand, timeout) }.answers { CompletableFuture.completedFuture(CommandResponse.Completed(Id.apply())) }

            assembly.submitAndWait(setupCommand, timeoutDuration)

            verify { assemblyCommandService.submitAndWait(setupCommand, timeout) }
        }

        @Test
        fun `submitAndWait should resolve commandService for given assembly and call submitAndWait method on it with defaultTimeout if timeout is not provided | ESW-121, ESW-245 `() = runBlocking {
            mockkStatic(CommandServiceFactory::class)
            every { locationServiceUtil.jResolveAkkaLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyLocation) }
            every { CommandServiceFactory.jMake(assemblyLocation, actorSystem) }.answers { assemblyCommandService }
            every { assemblyCommandService.submitAndWait(setupCommand, defaultTimeout) }.answers { CompletableFuture.completedFuture(CommandResponse.Completed(Id.apply())) }

            assembly.submitAndWait(setupCommand)

            verify { assemblyCommandService.submitAndWait(setupCommand, defaultTimeout) }
        }

        @Test
        fun `submitAndWait should resolve commandService for given assembly, call submitAndWait method on it and should't throw exception on negative submit response if resumeOnError=true | ESW-121, ESW-245 `() = runBlocking {
            val message = "error-occurred"
            val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

            mockkStatic(CommandServiceFactory::class)
            every { locationServiceUtil.jResolveAkkaLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyLocation) }
            every { CommandServiceFactory.jMake(assemblyLocation, actorSystem) }.answers { assemblyCommandService }
            every { assemblyCommandService.submitAndWait(setupCommand, timeout) }.answers { CompletableFuture.completedFuture(invalidSubmitResponse) }

            shouldNotThrow<CommandError> { assembly.submitAndWait(setupCommand, timeoutDuration, resumeOnError = true) }

            verify { assemblyCommandService.submitAndWait(setupCommand, timeout) }
        }

        @Test
        fun `submitAndWait should resolve commandService for given assembly, call submitAndWait method on it and should throw exception on negative submit response if resumeOnError=false | ESW-121, ESW-245, ESW-249 `() = runBlocking {
            val message = "error-occurred"
            val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

            mockkStatic(CommandServiceFactory::class)
            every { locationServiceUtil.jResolveAkkaLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyLocation) }
            every { CommandServiceFactory.jMake(assemblyLocation, actorSystem) }.answers { assemblyCommandService }
            every { assemblyCommandService.submitAndWait(setupCommand, timeout) }.answers { CompletableFuture.completedFuture(invalidSubmitResponse) }

            shouldThrow<CommandError> { assembly.submitAndWait(setupCommand, timeoutDuration) }

            verify { assemblyCommandService.submitAndWait(setupCommand, timeout) }
        }

        @Test
        fun `subscribeCurrentState should resolve commandService for given assembly and call subscribeCurrentState method on it | ESW-121, ESW-245 `() = runBlocking {
            val stateNames: Set<StateName> = mockk()
            val currentStateSubscription: Subscription = mockk()

            mockkStatic(CommandServiceFactory::class)
            every { locationServiceUtil.jResolveAkkaLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyLocation) }
            every { CommandServiceFactory.jMake(assemblyLocation, actorSystem) }.answers { assemblyCommandService }
            every { assemblyCommandService.subscribeCurrentState(stateNames, any()) }.answers { currentStateSubscription }

            assembly.subscribeCurrentState(stateNames) {}

            verify { assemblyCommandService.subscribeCurrentState(stateNames, any()) }
        }

        @Test
        fun `diagnosticMode should resolve actorRef for given assembly and send DiagnosticMode message to it | ESW-118, ESW-245 `() = runBlocking {
            val diagnosticMessage = DiagnosticDataMessage.DiagnosticMode(startTime, hint)

            every { locationServiceUtil.jResolveComponentRef(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyRef) }
            every { assemblyRef.tell(diagnosticMessage) }.answers { Unit }

            assembly.diagnosticMode(startTime, hint)

            verify { assemblyRef.tell(diagnosticMessage) }
        }

        @Test
        fun `operationsMode should resolve actorRef for given assembly and send OperationsMode message to it | ESW-118, ESW-245 `() = runBlocking {
            val operationsModeMessage = DiagnosticDataMessage.`OperationsMode$`.`MODULE$`

            every { locationServiceUtil.jResolveComponentRef(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyRef) }
            every { assemblyRef.tell(operationsModeMessage) }.answers { Unit }

            assembly.operationsMode()

            verify { assemblyRef.tell(operationsModeMessage) }
        }

        @Test
        fun `goOnline should resolve actorRef for given assembly and send GoOnline message to it | ESW-236, ESW-245 `() = runBlocking {
            val goOnlineMessage = RunningMessage.Lifecycle(ToComponentLifecycleMessage.`GoOnline$`.`MODULE$`)

            every { locationServiceUtil.jResolveComponentRef(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyRef) }
            every { assemblyRef.tell(goOnlineMessage) }.answers { Unit }

            assembly.goOnline()

            verify { assemblyRef.tell(goOnlineMessage) }
        }

        @Test
        fun `goOffline should resolve actorRef for given assembly and send GoOffline message to it | ESW-236, ESW-245 `() = runBlocking {
            val goOfflineMessage = RunningMessage.Lifecycle(ToComponentLifecycleMessage.`GoOffline$`.`MODULE$`)

            every { locationServiceUtil.jResolveComponentRef(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyRef) }
            every { assemblyRef.tell(goOfflineMessage) }.answers { Unit }

            assembly.goOffline()

            verify { assemblyRef.tell(goOfflineMessage) }
        }

        @Test
        fun `lock should resolve actorRef for given assembly and send Lock message to it | ESW-126, ESW-245 `() = runBlocking {
            every { locationServiceUtil.jResolveComponentRef(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyRef) }
            every { lockUnlockUtil.lock(assemblyRef, jLeaseDuration, any(), any()) }.answers { CompletableFuture.completedFuture(LockingResponse.`LockAcquired$`.`MODULE$`) }

            assembly.lock(leaseDuration, {}, {})

            verify { lockUnlockUtil.lock(assemblyRef, jLeaseDuration, any(), any()) }
        }

        @Test
        fun `unlock should resolve actorRef for given assembly and send Unlock message to it | ESW-126, ESW-245 `() = runBlocking {
            every { locationServiceUtil.jResolveComponentRef(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyRef) }
            every { lockUnlockUtil.unlock(assemblyRef) }.answers { CompletableFuture.completedFuture(LockingResponse.`LockReleased$`.`MODULE$`) }

            assembly.unlock()

            verify { lockUnlockUtil.unlock(assemblyRef) }
        }
    }

    @Nested
    inner class HCD {
        private val hcdName: String = "sampleHcd"
        private val componentType: ComponentType = JComponentType.HCD()
        private val prefix = Prefix(ESW(), hcdName)
        private val hcd: RichComponent =
                RichComponent(
                        prefix,
                        componentType,
                        lockUnlockUtil,
                        locationServiceUtil,
                        actorSystem,
                        defaultTimeoutDuration,
                        coroutineScope
                )

        private val hcdLocation: AkkaLocation = mockk()
        private val hcdRef: ActorRef<ComponentMessage> = mockk()
        private val hcdCommandService: ICommandService = mockk()


        @Test
        fun `validate should resolve commandService for given hcd and call validate method on it | ESW-121, ESW-245 `() = runBlocking {
            mockkStatic(CommandServiceFactory::class)
            every { locationServiceUtil.jResolveAkkaLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdLocation) }
            every { CommandServiceFactory.jMake(hcdLocation, actorSystem) }.answers { hcdCommandService }
            every { hcdCommandService.validate(setupCommand) }.answers { CompletableFuture.completedFuture(CommandResponse.Accepted(Id.apply())) }

            hcd.validate(setupCommand)

            verify { hcdCommandService.validate(setupCommand) }
        }

        @Test
        fun `oneway should resolve commandService for given hcd and call oneway method on it | ESW-121, ESW-245 `() = runBlocking {
            mockkStatic(CommandServiceFactory::class)
            every { locationServiceUtil.jResolveAkkaLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdLocation) }
            every { CommandServiceFactory.jMake(hcdLocation, actorSystem) }.answers { hcdCommandService }
            every { hcdCommandService.oneway(setupCommand) }.answers { CompletableFuture.completedFuture(CommandResponse.Accepted(Id.apply())) }

            hcd.oneway(setupCommand)

            verify { hcdCommandService.oneway(setupCommand) }
        }

        @Test
        fun `submit should resolve commandService for given hcd and call submit method on it | ESW-121, ESW-245 `() = runBlocking {
            mockkStatic(CommandServiceFactory::class)
            every { locationServiceUtil.jResolveAkkaLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdLocation) }
            every { CommandServiceFactory.jMake(hcdLocation, actorSystem) }.answers { hcdCommandService }
            every { hcdCommandService.submit(setupCommand) }.answers { CompletableFuture.completedFuture(CommandResponse.Completed(Id.apply())) }

            hcd.submit(setupCommand)

            verify { hcdCommandService.submit(setupCommand) }
        }

        @Test
        fun `query should resolve commandService for given hcd and call query method on it | ESW-121, ESW-245 `() = runBlocking {
            val commandRunId: Id = mockk()

            mockkStatic(CommandServiceFactory::class)
            every { locationServiceUtil.jResolveAkkaLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdLocation) }
            every { CommandServiceFactory.jMake(hcdLocation, actorSystem) }.answers { hcdCommandService }
            every { hcdCommandService.query(commandRunId) }.answers { CompletableFuture.completedFuture(CommandResponse.Completed(Id.apply())) }

            hcd.query(commandRunId)

            verify { hcdCommandService.query(commandRunId) }
        }

        @Test
        fun `queryFinal should resolve commandService for given hcd and call queryFinal method on it | ESW-121, ESW-245 `() = runBlocking {
            val commandRunId: Id = mockk()
            mockkStatic(CommandServiceFactory::class)
            every { locationServiceUtil.jResolveAkkaLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdLocation) }
            every { CommandServiceFactory.jMake(hcdLocation, actorSystem) }.answers { hcdCommandService }
            every { hcdCommandService.queryFinal(commandRunId, timeout) }.answers { CompletableFuture.completedFuture(CommandResponse.Completed(Id.apply())) }

            hcd.queryFinal(commandRunId, timeoutDuration)

            verify { hcdCommandService.queryFinal(commandRunId, timeout) }
        }

        @Test
        fun `queryFinal should resolve commandService for given hcd and call queryFinal method on it with defaultTimeout if timeout is not provided | ESW-121, ESW-245 `() = runBlocking {
            val commandRunId: Id = mockk()
            mockkStatic(CommandServiceFactory::class)
            every { locationServiceUtil.jResolveAkkaLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdLocation) }
            every { CommandServiceFactory.jMake(hcdLocation, actorSystem) }.answers { hcdCommandService }
            every { hcdCommandService.queryFinal(commandRunId, defaultTimeout) }.answers { CompletableFuture.completedFuture(CommandResponse.Completed(Id.apply())) }

            hcd.queryFinal(commandRunId)

            verify { hcdCommandService.queryFinal(commandRunId, defaultTimeout) }
        }

        @Test
        fun `submitAndWait should resolve commandService for given hcd and call submitAndWait method on it | ESW-121, ESW-245 `() = runBlocking {
            mockkStatic(CommandServiceFactory::class)
            every { locationServiceUtil.jResolveAkkaLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdLocation) }
            every { CommandServiceFactory.jMake(hcdLocation, actorSystem) }.answers { hcdCommandService }
            every { hcdCommandService.submitAndWait(setupCommand, timeout) }.answers { CompletableFuture.completedFuture(CommandResponse.Completed(Id.apply())) }

            hcd.submitAndWait(setupCommand, timeoutDuration)

            verify { hcdCommandService.submitAndWait(setupCommand, timeout) }
        }

        @Test
        fun `submitAndWait should resolve commandService for given hcd and call submitAndWait method on it with defaultTimeout if timeout is not provided | ESW-121, ESW-245 `() = runBlocking {
            mockkStatic(CommandServiceFactory::class)
            every { locationServiceUtil.jResolveAkkaLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdLocation) }
            every { CommandServiceFactory.jMake(hcdLocation, actorSystem) }.answers { hcdCommandService }
            every { hcdCommandService.submitAndWait(setupCommand, defaultTimeout) }.answers { CompletableFuture.completedFuture(CommandResponse.Completed(Id.apply())) }

            hcd.submitAndWait(setupCommand)

            verify { hcdCommandService.submitAndWait(setupCommand, defaultTimeout) }
        }

        @Test
        fun `submitAndWait should resolve commandService for given hcd, call submitAndWait method on it and should't throw exception on negative submit response if resumeOnError=true | ESW-121, ESW-245 `() = runBlocking {
            val message = "error-occurred"
            val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

            mockkStatic(CommandServiceFactory::class)
            every { locationServiceUtil.jResolveAkkaLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdLocation) }
            every { CommandServiceFactory.jMake(hcdLocation, actorSystem) }.answers { hcdCommandService }
            every { hcdCommandService.submitAndWait(setupCommand, timeout) }.answers { CompletableFuture.completedFuture(invalidSubmitResponse) }

            shouldNotThrow<CommandError> { hcd.submitAndWait(setupCommand, timeoutDuration, resumeOnError = true) }

            verify { hcdCommandService.submitAndWait(setupCommand, timeout) }
        }

        @Test
        fun `submitAndWait should resolve commandService for given hcd, call submitAndWait method on it and should throw exception on negative submit response if resumeOnError=false | ESW-121, ESW-245, ESW-249 `() = runBlocking {
            val message = "error-occurred"
            val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

            mockkStatic(CommandServiceFactory::class)
            every { locationServiceUtil.jResolveAkkaLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdLocation) }
            every { CommandServiceFactory.jMake(hcdLocation, actorSystem) }.answers { hcdCommandService }
            every { hcdCommandService.submitAndWait(setupCommand, timeout) }.answers { CompletableFuture.completedFuture(invalidSubmitResponse) }

            shouldThrow<CommandError> { hcd.submitAndWait(setupCommand, timeoutDuration) }

            verify { hcdCommandService.submitAndWait(setupCommand, timeout) }
        }

        @Test
        fun `subscribeCurrentState should resolve commandService for given hcd and call subscribeCurrentState method on it | ESW-121, ESW-245 `() = runBlocking {
            val stateNames: Set<StateName> = mockk()
            val currentStateSubscription: Subscription = mockk()

            mockkStatic(CommandServiceFactory::class)
            every { locationServiceUtil.jResolveAkkaLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdLocation) }
            every { CommandServiceFactory.jMake(hcdLocation, actorSystem) }.answers { hcdCommandService }
            every { hcdCommandService.subscribeCurrentState(stateNames, any()) }.answers { currentStateSubscription }

            hcd.subscribeCurrentState(stateNames) {}

            verify { hcdCommandService.subscribeCurrentState(stateNames, any()) }
        }

        @Test
        fun `diagnosticMode should resolve actorRef for given hcd and send DiagnosticMode message to it | ESW-118, ESW-245 `() = runBlocking {
            val diagnosticMessage = DiagnosticDataMessage.DiagnosticMode(startTime, hint)

            every { locationServiceUtil.jResolveComponentRef(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdRef) }
            every { hcdRef.tell(diagnosticMessage) }.answers { Unit }

            hcd.diagnosticMode(startTime, hint)

            verify { hcdRef.tell(diagnosticMessage) }
        }

        @Test
        fun `operationsMode should resolve actorRef for given hcd and send OperationsMode message to it | ESW-118, ESW-245 `() = runBlocking {
            val operationsModeMessage = DiagnosticDataMessage.`OperationsMode$`.`MODULE$`

            every { locationServiceUtil.jResolveComponentRef(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdRef) }
            every { hcdRef.tell(operationsModeMessage) }.answers { Unit }

            hcd.operationsMode()

            verify { hcdRef.tell(operationsModeMessage) }
        }

        @Test
        fun `goOnline should resolve actorRef for given hcd and send GoOnline message to it | ESW-236, ESW-245 `() = runBlocking {
            val goOnlineMessage = RunningMessage.Lifecycle(ToComponentLifecycleMessage.`GoOnline$`.`MODULE$`)

            every { locationServiceUtil.jResolveComponentRef(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdRef) }
            every { hcdRef.tell(goOnlineMessage) }.answers { Unit }

            hcd.goOnline()

            verify { hcdRef.tell(goOnlineMessage) }
        }

        @Test
        fun `goOffline should resolve actorRef for given hcd and send GoOffline message to it | ESW-236, ESW-245 `() = runBlocking {
            val goOfflineMessage = RunningMessage.Lifecycle(ToComponentLifecycleMessage.`GoOffline$`.`MODULE$`)

            every { locationServiceUtil.jResolveComponentRef(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdRef) }
            every { hcdRef.tell(goOfflineMessage) }.answers { Unit }

            hcd.goOffline()

            verify { hcdRef.tell(goOfflineMessage) }
        }

        @Test
        fun `lock should resolve actorRef for given hcd and send Lock message to it | ESW-126, ESW-245 `() = runBlocking {
            every { locationServiceUtil.jResolveComponentRef(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdRef) }
            every { lockUnlockUtil.lock(hcdRef, jLeaseDuration, any(), any()) }.answers { CompletableFuture.completedFuture(LockingResponse.`LockAcquired$`.`MODULE$`) }

            hcd.lock(leaseDuration)

            verify { lockUnlockUtil.lock(hcdRef, jLeaseDuration, any(), any()) }
        }

        @Test
        fun `unlock should resolve actorRef for given hcd and send Unlock message to it | ESW-126, ESW-245 `() = runBlocking {
            every { locationServiceUtil.jResolveComponentRef(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdRef) }
            every { lockUnlockUtil.unlock(hcdRef) }.answers { CompletableFuture.completedFuture(LockingResponse.`LockReleased$`.`MODULE$`) }

            hcd.unlock()

            verify { lockUnlockUtil.unlock(hcdRef) }
        }
    }

}

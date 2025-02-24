package esw.ocs.dsl.highlevel

import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.util.Timeout
import csw.command.api.javadsl.ICommandService
import csw.command.client.CommandServiceFactory
import csw.command.client.messages.ComponentMessage
import csw.command.client.messages.DiagnosticDataMessage
import csw.command.client.messages.RunningMessage
import csw.command.client.models.framework.LockingResponse
import csw.command.client.models.framework.ToComponentLifecycleMessage
import csw.location.api.models.PekkoLocation
import csw.location.api.models.ComponentType
import csw.params.commands.CommandName
import csw.params.commands.CommandResponse
import csw.params.commands.Setup
import csw.params.core.models.Id
import csw.params.core.models.ObsId
import csw.params.core.states.StateName
import csw.prefix.models.Prefix
import csw.time.core.models.UTCTime
import esw.ocs.dsl.highlevel.models.Assembly
import esw.ocs.dsl.highlevel.models.CommandError
import esw.ocs.dsl.highlevel.models.ESW
import esw.ocs.dsl.highlevel.models.HCD
import esw.ocs.dsl.script.utils.CommandUtil
import esw.ocs.dsl.script.utils.LockUnlockUtil
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import msocket.api.Subscription
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Suppress("DANGEROUS_CHARACTERS")
class RichComponentTest {
    private val coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)

    private val hint = "test-hint"
    private val startTime: UTCTime = UTCTime.now()

    private val source = Prefix(ESW, "test")
    private val setupCommand = Setup(source, CommandName("move"), Optional.of(ObsId.apply("2020A-001-123")))

    private val leaseDuration: Duration = 10.seconds
    private val jLeaseDuration: java.time.Duration = leaseDuration.toJavaDuration()

    private val lockUnlockUtil: LockUnlockUtil = mockk()
    private val commandUtil: CommandUtil = mockk()
    private val actorSystem: ActorSystem<*> = mockk()

    private val timeoutDuration: Duration = 5.seconds
    private val timeout = Timeout(timeoutDuration.inWholeNanoseconds, TimeUnit.NANOSECONDS)

    private val defaultTimeoutDuration: Duration = 5.seconds
    private val defaultTimeout = Timeout(defaultTimeoutDuration.inWholeNanoseconds, TimeUnit.NANOSECONDS)

    @Nested
    inner class Assembly {
        private val componentName = "sampleAssembly"
        private val componentType = Assembly
        private val prefix = Prefix(ESW, componentName)
        private val assembly: RichComponent =
                RichComponent(
                        prefix,
                        componentType,
                        lockUnlockUtil,
                        commandUtil,
                        actorSystem,
                        defaultTimeoutDuration,
                        coroutineScope
                )

        private val assemblyLocation: PekkoLocation = mockk()
        private val assemblyRef: ActorRef<ComponentMessage> = mockk()
        private val assemblyCommandService: ICommandService = mockk()

        @BeforeEach
        fun beforeEach() {
            mockkStatic(CommandServiceFactory::class)
        }

        @Test
        fun `validate_should_resolve_commandService_for_given_assembly_and_call_validate_method_on_it_|_ESW-121,_ESW-245_`() = runBlocking {

            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyLocation) }
            every { CommandServiceFactory.jMake(assemblyLocation, actorSystem) }.answers { assemblyCommandService }
            every { assemblyCommandService.validate(setupCommand) }.answers { CompletableFuture.completedFuture(CommandResponse.Accepted(Id.apply())) }

            assembly.validate(setupCommand)

            verify { assemblyCommandService.validate(setupCommand) }
        }

        @Test
        fun `oneway_should_resolve_commandService_for_given_assembly_and_call_oneway_method_on_it_|_ESW-121,_ESW-245_`() = runBlocking {
            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyLocation) }
            every { CommandServiceFactory.jMake(assemblyLocation, actorSystem) }.answers { assemblyCommandService }
            every { assemblyCommandService.oneway(setupCommand) }.answers { CompletableFuture.completedFuture(CommandResponse.Accepted(Id.apply())) }

            assembly.oneway(setupCommand)

            verify { assemblyCommandService.oneway(setupCommand) }
        }

        @Test
        fun `submit_should_resolve_commandService_for_given_assembly_and_call_submit_method_on_it_|_ESW-121,_ESW-245_`() = runBlocking {
            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyLocation) }
            every { CommandServiceFactory.jMake(assemblyLocation, actorSystem) }.answers { assemblyCommandService }
            every { assemblyCommandService.submit(setupCommand) }.answers { CompletableFuture.completedFuture(CommandResponse.Completed(Id.apply())) }

            assembly.submit(setupCommand)

            verify { assemblyCommandService.submit(setupCommand) }
        }

        @Test
        fun `submit_should_not_throw_exception_on_negative_submit_response_if_resumeOnError=true_|_ESW-121,_ESW-245,_ESW-139,_ESW-249_`() = runBlocking {
            val message = "error-occurred"
            val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyLocation) }
            every { CommandServiceFactory.jMake(assemblyLocation, actorSystem) }.answers { assemblyCommandService }
            every { assemblyCommandService.submit(setupCommand) }.answers { CompletableFuture.completedFuture(invalidSubmitResponse) }

            shouldNotThrow<CommandError> { assembly.submit(setupCommand, resumeOnError = true) }

            verify { assemblyCommandService.submit(setupCommand) }
        }

        @Test
        fun `submit_should_throw_exception_on_negative_submit_response_if_resumeOnError=false_|_ESW-121,_ESW-245,_ESW-139,_ESW-249_`() = runBlocking {
            val message = "error-occurred"
            val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyLocation) }
            every { CommandServiceFactory.jMake(assemblyLocation, actorSystem) }.answers { assemblyCommandService }
            every { assemblyCommandService.submit(setupCommand) }.answers { CompletableFuture.completedFuture(invalidSubmitResponse) }

            shouldThrow<CommandError> { assembly.submit(setupCommand) }

            verify { assemblyCommandService.submit(setupCommand) }
        }

        @Test
        fun `query_should_resolve_commandService_for_given_assembly_and_call_query_method_on_it_|_ESW-121,_ESW-245_`() = runBlocking {
            val commandRunId: Id = mockk()

            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyLocation) }
            every { CommandServiceFactory.jMake(assemblyLocation, actorSystem) }.answers { assemblyCommandService }
            every { assemblyCommandService.query(commandRunId) }.answers { CompletableFuture.completedFuture(CommandResponse.Completed(Id.apply())) }

            assembly.query(commandRunId)

            verify { assemblyCommandService.query(commandRunId) }
        }

        @Test
        fun `query_should_not_throw_exception_on_negative_submit_response_if_resumeOnError=true_|_ESW-121,_ESW-245,_ESW-139,_ESW-249_`() = runBlocking {
            val commandRunId: Id = mockk()
            val message = "error-occurred"
            val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyLocation) }
            every { CommandServiceFactory.jMake(assemblyLocation, actorSystem) }.answers { assemblyCommandService }
            every { assemblyCommandService.query(commandRunId) }.answers { CompletableFuture.completedFuture(invalidSubmitResponse) }

            shouldNotThrow<CommandError> { assembly.query(commandRunId, resumeOnError = true) }

            verify { assemblyCommandService.query(commandRunId) }
        }

        @Test
        fun `query_should_throw_exception_on_negative_submit_response_if_resumeOnError=false_|_ESW-121,_ESW-245,_ESW-139,_ESW-249_`() = runBlocking {
            val commandRunId: Id = mockk()
            val message = "error-occurred"
            val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyLocation) }
            every { CommandServiceFactory.jMake(assemblyLocation, actorSystem) }.answers { assemblyCommandService }
            every { assemblyCommandService.query(commandRunId) }.answers { CompletableFuture.completedFuture(invalidSubmitResponse) }

            shouldThrow<CommandError> { assembly.query(commandRunId) }

            verify { assemblyCommandService.query(commandRunId) }
        }

        @Test
        fun `queryFinal_should_resolve_commandService_and_call_queryFinal_method_|_ESW-121,_ESW-245_`() = runBlocking {
            val commandRunId: Id = mockk()

            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyLocation) }
            every { CommandServiceFactory.jMake(assemblyLocation, actorSystem) }.answers { assemblyCommandService }
            every { assemblyCommandService.queryFinal(commandRunId, timeout) }.answers { CompletableFuture.completedFuture(CommandResponse.Completed(Id.apply())) }

            assembly.queryFinal(commandRunId, timeoutDuration)

            verify { assemblyCommandService.queryFinal(commandRunId, timeout) }
        }

        @Test
        fun `queryFinal_should_use_defaultTimeout_if_timeout_is_not_provided_|_ESW-121,_ESW-245,_ESW-139,_ESW-249_`() = runBlocking {
            val commandRunId: Id = mockk()

            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyLocation) }
            every { CommandServiceFactory.jMake(assemblyLocation, actorSystem) }.answers { assemblyCommandService }
            every { assemblyCommandService.queryFinal(commandRunId, defaultTimeout) }.answers { CompletableFuture.completedFuture(CommandResponse.Completed(Id.apply())) }

            assembly.queryFinal(commandRunId)

            verify { assemblyCommandService.queryFinal(commandRunId, defaultTimeout) }
        }

        @Test
        fun `queryFinal_should_not_throw_exception_on_negative_submit_response_if_resumeOnError=true_|_ESW-121,_ESW-245,_ESW-139,_ESW-249_`() = runBlocking {
            val commandRunId: Id = mockk()
            val message = "error-occurred"
            val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyLocation) }
            every { CommandServiceFactory.jMake(assemblyLocation, actorSystem) }.answers { assemblyCommandService }
            every { assemblyCommandService.queryFinal(commandRunId, timeout) }.answers { CompletableFuture.completedFuture(invalidSubmitResponse) }

            shouldNotThrow<CommandError> { assembly.queryFinal(commandRunId, timeoutDuration, resumeOnError = true) }

            verify { assemblyCommandService.queryFinal(commandRunId, timeout) }
        }

        @Test
        fun `queryFinal_should_throw_exception_on_negative_submit_response_if_resumeOnError=false_|_ESW-121,_ESW-245,_ESW-139,_ESW-249_`() = runBlocking {
            val commandRunId: Id = mockk()
            val message = "error-occurred"
            val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyLocation) }
            every { CommandServiceFactory.jMake(assemblyLocation, actorSystem) }.answers { assemblyCommandService }
            every { assemblyCommandService.queryFinal(commandRunId, timeout) }.answers { CompletableFuture.completedFuture(invalidSubmitResponse) }

            shouldThrow<CommandError> { assembly.queryFinal(commandRunId, timeoutDuration) }

            verify { assemblyCommandService.queryFinal(commandRunId, timeout) }
        }

        @Test
        fun `submitAndWait_should_resolve_commandService_for_given_assembly_and_call_submitAndWait_method_on_it_|_ESW-121,_ESW-245_`() = runBlocking {
            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyLocation) }
            every { CommandServiceFactory.jMake(assemblyLocation, actorSystem) }.answers { assemblyCommandService }
            every { assemblyCommandService.submitAndWait(setupCommand, timeout) }.answers { CompletableFuture.completedFuture(CommandResponse.Completed(Id.apply())) }

            assembly.submitAndWait(setupCommand, timeoutDuration)

            verify { assemblyCommandService.submitAndWait(setupCommand, timeout) }
        }

        @Test
        fun `submitAndWait_should_use_defaultTimeout_if_timeout_is_not_provided_|_ESW-121,_ESW-245_`() = runBlocking {
            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyLocation) }
            every { CommandServiceFactory.jMake(assemblyLocation, actorSystem) }.answers { assemblyCommandService }
            every { assemblyCommandService.submitAndWait(setupCommand, defaultTimeout) }.answers { CompletableFuture.completedFuture(CommandResponse.Completed(Id.apply())) }

            assembly.submitAndWait(setupCommand)

            verify { assemblyCommandService.submitAndWait(setupCommand, defaultTimeout) }
        }

        @Test
        fun `submitAndWait_should_not_throw_exception_on_negative_submit_response_if_resumeOnError=true_|_ESW-121,_ESW-245,_ESW-139,_ESW-249_`() = runBlocking {
            val message = "error-occurred"
            val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyLocation) }
            every { CommandServiceFactory.jMake(assemblyLocation, actorSystem) }.answers { assemblyCommandService }
            every { assemblyCommandService.submitAndWait(setupCommand, timeout) }.answers { CompletableFuture.completedFuture(invalidSubmitResponse) }

            shouldNotThrow<CommandError> { assembly.submitAndWait(setupCommand, timeoutDuration, resumeOnError = true) }

            verify { assemblyCommandService.submitAndWait(setupCommand, timeout) }
        }

        @Test
        fun `submitAndWait_should_throw_exception_on_negative_submit_response_if_resumeOnError=false_|_ESW-121,_ESW-245,_ESW-139,_ESW-249_`() = runBlocking {
            val message = "error-occurred"
            val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyLocation) }
            every { CommandServiceFactory.jMake(assemblyLocation, actorSystem) }.answers { assemblyCommandService }
            every { assemblyCommandService.submitAndWait(setupCommand, timeout) }.answers { CompletableFuture.completedFuture(invalidSubmitResponse) }

            shouldThrow<CommandError> { assembly.submitAndWait(setupCommand, timeoutDuration) }

            verify { assemblyCommandService.submitAndWait(setupCommand, timeout) }
        }

        @Test
        fun `subscribeCurrentState_should_resolve_commandService_for_given_assembly_and_call_subscribeCurrentState_method_on_it_|_ESW-121,_ESW-245_`() = runBlocking {
            val stateName = mockk<StateName>()
            val stateNames = setOf(stateName)
            val currentStateSubscription: Subscription = mockk()

            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyLocation) }
            every { CommandServiceFactory.jMake(assemblyLocation, actorSystem) }.answers { assemblyCommandService }
            every { assemblyCommandService.subscribeCurrentState(stateNames, any()) }.answers { currentStateSubscription }

            assembly.subscribeCurrentState(stateName) {}

            verify { assemblyCommandService.subscribeCurrentState(stateNames, any()) }
        }

        @Test
        fun `diagnosticMode_should_resolve_actorRef_for_given_assembly_and_send_DiagnosticMode_message_to_it_|_ESW-118,_ESW-245_`() = runBlocking {
            val diagnosticMessage = DiagnosticDataMessage.DiagnosticMode(startTime, hint)

            every { commandUtil.jResolveComponentRef(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyRef) }
            every { assemblyRef.tell(diagnosticMessage) }.answers { Unit }

            assembly.diagnosticMode(startTime, hint)

            verify { assemblyRef.tell(diagnosticMessage) }
        }

        @Test
        fun `operationsMode_should_resolve_actorRef_for_given_assembly_and_send_OperationsMode_message_to_it_|_ESW-118,_ESW-245_`() = runBlocking {
            val operationsModeMessage = DiagnosticDataMessage.`OperationsMode$`.`MODULE$`

            every { commandUtil.jResolveComponentRef(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyRef) }
            every { assemblyRef.tell(operationsModeMessage) }.answers { Unit }

            assembly.operationsMode()

            verify { assemblyRef.tell(operationsModeMessage) }
        }

        @Test
        fun `goOnline_should_resolve_actorRef_for_given_assembly_and_send_GoOnline_message_to_it_|_ESW-236,_ESW-245_`() = runBlocking {
            val goOnlineMessage = RunningMessage.Lifecycle(ToComponentLifecycleMessage.`GoOnline$`.`MODULE$`)

            every { commandUtil.jResolveComponentRef(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyRef) }
            every { assemblyRef.tell(goOnlineMessage) }.answers { Unit }

            assembly.goOnline()

            verify { assemblyRef.tell(goOnlineMessage) }
        }

        @Test
        fun `goOffline_should_resolve_actorRef_for_given_assembly_and_send_GoOffline_message_to_it_|_ESW-236,_ESW-245_`() = runBlocking {
            val goOfflineMessage = RunningMessage.Lifecycle(ToComponentLifecycleMessage.`GoOffline$`.`MODULE$`)

            every { commandUtil.jResolveComponentRef(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyRef) }
            every { assemblyRef.tell(goOfflineMessage) }.answers { Unit }

            assembly.goOffline()

            verify { assemblyRef.tell(goOfflineMessage) }
        }

        @Test
        fun `lock_should_resolve_actorRef_for_given_assembly_and_send_Lock_message_to_it_|_ESW-126,_ESW-245_`() = runBlocking {
            every { commandUtil.jResolveComponentRef(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyRef) }
            every { lockUnlockUtil.lock(assemblyRef, jLeaseDuration, any(), any()) }.answers { CompletableFuture.completedFuture(LockingResponse.`LockAcquired$`.`MODULE$`) }

            assembly.lock(leaseDuration, {}, {})

            verify { lockUnlockUtil.lock(assemblyRef, jLeaseDuration, any(), any()) }
        }

        @Test
        fun `unlock_should_resolve_actorRef_for_given_assembly_and_send_Unlock_message_to_it_|_ESW-126,_ESW-245_`() = runBlocking {
            every { commandUtil.jResolveComponentRef(prefix, componentType) }.answers { CompletableFuture.completedFuture(assemblyRef) }
            every { lockUnlockUtil.unlock(assemblyRef) }.answers { CompletableFuture.completedFuture(LockingResponse.`LockReleased$`.`MODULE$`) }

            assembly.unlock()

            verify { lockUnlockUtil.unlock(assemblyRef) }
        }
    }

    @Nested
    inner class HCD {
        private val hcdName: String = "sampleHcd"
        private val componentType: ComponentType = HCD
        private val prefix = Prefix(ESW, hcdName)
        private val hcd: RichComponent =
                RichComponent(
                        prefix,
                        componentType,
                        lockUnlockUtil,
                        commandUtil,
                        actorSystem,
                        defaultTimeoutDuration,
                        coroutineScope
                )

        private val hcdLocation: PekkoLocation = mockk()
        private val hcdRef: ActorRef<ComponentMessage> = mockk()
        private val hcdCommandService: ICommandService = mockk()

        @BeforeEach
        fun beforeEach() {
            mockkStatic(CommandServiceFactory::class)
        }

        @Test
        fun `validate_should_resolve_commandService_for_given_hcd_and_call_validate_method_on_it_|_ESW-121,_ESW-245_`() = runBlocking {
            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdLocation) }
            every { CommandServiceFactory.jMake(hcdLocation, actorSystem) }.answers { hcdCommandService }
            every { hcdCommandService.validate(setupCommand) }.answers { CompletableFuture.completedFuture(CommandResponse.Accepted(Id.apply())) }

            hcd.validate(setupCommand)

            verify { hcdCommandService.validate(setupCommand) }
        }

        @Test
        fun `oneway_should_resolve_commandService_for_given_hcd_and_call_oneway_method_on_it_|_ESW-121,_ESW-245_`() = runBlocking {
            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdLocation) }
            every { CommandServiceFactory.jMake(hcdLocation, actorSystem) }.answers { hcdCommandService }
            every { hcdCommandService.oneway(setupCommand) }.answers { CompletableFuture.completedFuture(CommandResponse.Accepted(Id.apply())) }

            hcd.oneway(setupCommand)

            verify { hcdCommandService.oneway(setupCommand) }
        }

        @Test
        fun `submit_should_resolve_commandService_for_given_hcd_and_call_submit_method_on_it_|_ESW-121,_ESW-245_`() = runBlocking {
            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdLocation) }
            every { CommandServiceFactory.jMake(hcdLocation, actorSystem) }.answers { hcdCommandService }
            every { hcdCommandService.submit(setupCommand) }.answers { CompletableFuture.completedFuture(CommandResponse.Completed(Id.apply())) }

            hcd.submit(setupCommand)

            verify { hcdCommandService.submit(setupCommand) }
        }

        @Test
        fun `submit_should_resolve_commandService_for_given_hcd,_call_submit_method_on_it_and_should't_throw_exception_on_negative_submit_response_if_resumeOnError=true_|_ESW-121,_ESW-245,_ESW-139,_ESW-249_`() = runBlocking {
            val message = "error-occurred"
            val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdLocation) }
            every { CommandServiceFactory.jMake(hcdLocation, actorSystem) }.answers { hcdCommandService }
            every { hcdCommandService.submit(setupCommand) }.answers { CompletableFuture.completedFuture(invalidSubmitResponse) }

            shouldNotThrow<CommandError> { hcd.submit(setupCommand, resumeOnError = true) }

            verify { hcdCommandService.submit(setupCommand) }
        }

        @Test
        fun `submit_should_resolve_commandService_for_given_hcd,_call_submit_method_on_it_and_should_throw_exception_on_negative_submit_response_if_resumeOnError=false_|_ESW-121,_ESW-245,_ESW-139,_ESW-249_`() = runBlocking {
            val message = "error-occurred"
            val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdLocation) }
            every { CommandServiceFactory.jMake(hcdLocation, actorSystem) }.answers { hcdCommandService }
            every { hcdCommandService.submit(setupCommand) }.answers { CompletableFuture.completedFuture(invalidSubmitResponse) }

            shouldThrow<CommandError> { hcd.submit(setupCommand) }

            verify { hcdCommandService.submit(setupCommand) }
        }

        @Test
        fun `query_should_resolve_commandService_for_given_hcd_and_call_query_method_on_it_|_ESW-121,_ESW-245_`() = runBlocking {
            val commandRunId: Id = mockk()

            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdLocation) }
            every { CommandServiceFactory.jMake(hcdLocation, actorSystem) }.answers { hcdCommandService }
            every { hcdCommandService.query(commandRunId) }.answers { CompletableFuture.completedFuture(CommandResponse.Completed(Id.apply())) }

            hcd.query(commandRunId)

            verify { hcdCommandService.query(commandRunId) }
        }

        @Test
        fun `query_should_resolve_commandService_for_given_hcd,_call_query_method_on_it_and_should't_throw_exception_on_negative_submit_response_if_resumeOnError=true_|_ESW-121,_ESW-245,_ESW-139,_ESW-249_`() = runBlocking {
            val commandRunId: Id = mockk()
            val message = "error-occurred"
            val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdLocation) }
            every { CommandServiceFactory.jMake(hcdLocation, actorSystem) }.answers { hcdCommandService }
            every { hcdCommandService.query(commandRunId) }.answers { CompletableFuture.completedFuture(invalidSubmitResponse) }

            shouldNotThrow<CommandError> { hcd.query(commandRunId, resumeOnError = true) }

            verify { hcdCommandService.query(commandRunId) }
        }

        @Test
        fun `query_should_resolve_commandService_for_given_hcd,_call_query_method_on_it_and_should_throw_exception_on_negative_submit_response_if_resumeOnError=false_|_ESW-121,_ESW-245,_ESW-139,_ESW-249_`() = runBlocking {
            val commandRunId: Id = mockk()
            val message = "error-occurred"
            val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdLocation) }
            every { CommandServiceFactory.jMake(hcdLocation, actorSystem) }.answers { hcdCommandService }
            every { hcdCommandService.query(commandRunId) }.answers { CompletableFuture.completedFuture(invalidSubmitResponse) }

            shouldThrow<CommandError> { hcd.query(commandRunId) }

            verify { hcdCommandService.query(commandRunId) }
        }

        @Test
        fun `queryFinal_should_resolve_commandService_for_given_hcd_and_call_queryFinal_method_on_it_|_ESW-121,_ESW-245_`() = runBlocking {
            val commandRunId: Id = mockk()
            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdLocation) }
            every { CommandServiceFactory.jMake(hcdLocation, actorSystem) }.answers { hcdCommandService }
            every { hcdCommandService.queryFinal(commandRunId, timeout) }.answers { CompletableFuture.completedFuture(CommandResponse.Completed(Id.apply())) }

            hcd.queryFinal(commandRunId, timeoutDuration)

            verify { hcdCommandService.queryFinal(commandRunId, timeout) }
        }

        @Test
        fun `queryFinal_should_use_defaultTimeout_if_timeout_is_not_provided_|_ESW-121,_ESW-245_`() = runBlocking {
            val commandRunId: Id = mockk()
            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdLocation) }
            every { CommandServiceFactory.jMake(hcdLocation, actorSystem) }.answers { hcdCommandService }
            every { hcdCommandService.queryFinal(commandRunId, defaultTimeout) }.answers { CompletableFuture.completedFuture(CommandResponse.Completed(Id.apply())) }

            hcd.queryFinal(commandRunId)

            verify { hcdCommandService.queryFinal(commandRunId, defaultTimeout) }
        }

        @Test
        fun `queryFinal_should_not_throw_exception_on_negative_submit_response_if_resumeOnError=true_|_ESW-121,_ESW-245,_ESW-139,_ESW-249_`() = runBlocking {
            val commandRunId: Id = mockk()
            val message = "error-occurred"
            val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdLocation) }
            every { CommandServiceFactory.jMake(hcdLocation, actorSystem) }.answers { hcdCommandService }
            every { hcdCommandService.queryFinal(commandRunId, timeout) }.answers { CompletableFuture.completedFuture(invalidSubmitResponse) }

            shouldNotThrow<CommandError> { hcd.queryFinal(commandRunId, timeoutDuration, resumeOnError = true) }

            verify { hcdCommandService.queryFinal(commandRunId, timeout) }
        }

        @Test
        fun `queryFinal_should_throw_exception_on_negative_submit_response_if_resumeOnError=false_|_ESW-121,_ESW-245,_ESW-139,_ESW-249_`() = runBlocking {
            val commandRunId: Id = mockk()
            val message = "error-occurred"
            val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdLocation) }
            every { CommandServiceFactory.jMake(hcdLocation, actorSystem) }.answers { hcdCommandService }
            every { hcdCommandService.queryFinal(commandRunId, timeout) }.answers { CompletableFuture.completedFuture(invalidSubmitResponse) }

            shouldThrow<CommandError> { hcd.queryFinal(commandRunId, timeoutDuration) }

            verify { hcdCommandService.queryFinal(commandRunId, timeout) }
        }

        @Test
        fun `submitAndWait_should_resolve_commandService_for_given_hcd_and_call_submitAndWait_method_on_it_|_ESW-121,_ESW-245_`() = runBlocking {
            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdLocation) }
            every { CommandServiceFactory.jMake(hcdLocation, actorSystem) }.answers { hcdCommandService }
            every { hcdCommandService.submitAndWait(setupCommand, timeout) }.answers { CompletableFuture.completedFuture(CommandResponse.Completed(Id.apply())) }

            hcd.submitAndWait(setupCommand, timeoutDuration)

            verify { hcdCommandService.submitAndWait(setupCommand, timeout) }
        }

        @Test
        fun `submitAndWait_should_use_defaultTimeout_if_timeout_is_not_provided_|_ESW-121,_ESW-245_`() = runBlocking {
            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdLocation) }
            every { CommandServiceFactory.jMake(hcdLocation, actorSystem) }.answers { hcdCommandService }
            every { hcdCommandService.submitAndWait(setupCommand, defaultTimeout) }.answers { CompletableFuture.completedFuture(CommandResponse.Completed(Id.apply())) }

            hcd.submitAndWait(setupCommand)

            verify { hcdCommandService.submitAndWait(setupCommand, defaultTimeout) }
        }

        @Test
        fun `submitAndWait_should_not_throw_exception_on_negative_submit_response_if_resumeOnError=true_|_ESW-121,_ESW-245,_ESW-139,_ESW-249_`() = runBlocking {
            val message = "error-occurred"
            val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdLocation) }
            every { CommandServiceFactory.jMake(hcdLocation, actorSystem) }.answers { hcdCommandService }
            every { hcdCommandService.submitAndWait(setupCommand, timeout) }.answers { CompletableFuture.completedFuture(invalidSubmitResponse) }

            shouldNotThrow<CommandError> { hcd.submitAndWait(setupCommand, timeoutDuration, resumeOnError = true) }

            verify { hcdCommandService.submitAndWait(setupCommand, timeout) }
        }

        @Test
        fun `submitAndWait_should_throw_exception_on_negative_submit_response_if_resumeOnError=false_|_ESW-121,_ESW-245,_ESW-139,_ESW-249_`() = runBlocking {
            val message = "error-occurred"
            val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdLocation) }
            every { CommandServiceFactory.jMake(hcdLocation, actorSystem) }.answers { hcdCommandService }
            every { hcdCommandService.submitAndWait(setupCommand, timeout) }.answers { CompletableFuture.completedFuture(invalidSubmitResponse) }

            shouldThrow<CommandError> { hcd.submitAndWait(setupCommand, timeoutDuration) }

            verify { hcdCommandService.submitAndWait(setupCommand, timeout) }
        }

        @Test
        fun `subscribeCurrentState_should_resolve_commandService_for_given_hcd_and_call_subscribeCurrentState_method_on_it_|_ESW-121,_ESW-245_`() = runBlocking {
            val stateName = mockk<StateName>()
            val stateNames = setOf(stateName)
            val currentStateSubscription: Subscription = mockk()

            every { commandUtil.jResolvePekkoLocation(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdLocation) }
            every { CommandServiceFactory.jMake(hcdLocation, actorSystem) }.answers { hcdCommandService }
            every { hcdCommandService.subscribeCurrentState(stateNames, any()) }.answers { currentStateSubscription }

            hcd.subscribeCurrentState(stateName) {}

            verify { hcdCommandService.subscribeCurrentState(stateNames, any()) }
        }

        @Test
        fun `diagnosticMode_should_resolve_actorRef_for_given_hcd_and_send_DiagnosticMode_message_to_it_|_ESW-118,_ESW-245_`() = runBlocking {
            val diagnosticMessage = DiagnosticDataMessage.DiagnosticMode(startTime, hint)

            every { commandUtil.jResolveComponentRef(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdRef) }
            every { hcdRef.tell(diagnosticMessage) }.answers { Unit }

            hcd.diagnosticMode(startTime, hint)

            verify { hcdRef.tell(diagnosticMessage) }
        }

        @Test
        fun `operationsMode_should_resolve_actorRef_for_given_hcd_and_send_OperationsMode_message_to_it_|_ESW-118,_ESW-245_`() = runBlocking {
            val operationsModeMessage = DiagnosticDataMessage.`OperationsMode$`.`MODULE$`

            every { commandUtil.jResolveComponentRef(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdRef) }
            every { hcdRef.tell(operationsModeMessage) }.answers { Unit }

            hcd.operationsMode()

            verify { hcdRef.tell(operationsModeMessage) }
        }

        @Test
        fun `goOnline_should_resolve_actorRef_for_given_hcd_and_send_GoOnline_message_to_it_|_ESW-236,_ESW-245_`() = runBlocking {
            val goOnlineMessage = RunningMessage.Lifecycle(ToComponentLifecycleMessage.`GoOnline$`.`MODULE$`)

            every { commandUtil.jResolveComponentRef(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdRef) }
            every { hcdRef.tell(goOnlineMessage) }.answers { Unit }

            hcd.goOnline()

            verify { hcdRef.tell(goOnlineMessage) }
        }

        @Test
        fun `goOffline_should_resolve_actorRef_for_given_hcd_and_send_GoOffline_message_to_it_|_ESW-236,_ESW-245_`() = runBlocking {
            val goOfflineMessage = RunningMessage.Lifecycle(ToComponentLifecycleMessage.`GoOffline$`.`MODULE$`)

            every { commandUtil.jResolveComponentRef(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdRef) }
            every { hcdRef.tell(goOfflineMessage) }.answers { Unit }

            hcd.goOffline()

            verify { hcdRef.tell(goOfflineMessage) }
        }

        @Test
        fun `lock_should_resolve_actorRef_for_given_hcd_and_send_Lock_message_to_it_|_ESW-126,_ESW-245_`() = runBlocking {
            every { commandUtil.jResolveComponentRef(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdRef) }
            every { lockUnlockUtil.lock(hcdRef, jLeaseDuration, any(), any()) }.answers { CompletableFuture.completedFuture(LockingResponse.`LockAcquired$`.`MODULE$`) }

            hcd.lock(leaseDuration)

            verify { lockUnlockUtil.lock(hcdRef, jLeaseDuration, any(), any()) }
        }

        @Test
        fun `unlock_should_resolve_actorRef_for_given_hcd_and_send_Unlock_message_to_it_|_ESW-126,_ESW-245_`() = runBlocking {
            every { commandUtil.jResolveComponentRef(prefix, componentType) }.answers { CompletableFuture.completedFuture(hcdRef) }
            every { lockUnlockUtil.unlock(hcdRef) }.answers { CompletableFuture.completedFuture(LockingResponse.`LockReleased$`.`MODULE$`) }

            hcd.unlock()

            verify { lockUnlockUtil.unlock(hcdRef) }
        }
    }

}

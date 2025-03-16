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
import csw.location.api.models.ComponentType
import csw.params.commands.CommandResponse.*
import csw.params.commands.ControlCommand
import csw.params.core.models.Id
import csw.params.core.states.CurrentState
import csw.params.core.states.StateName
import csw.prefix.models.Prefix
import csw.time.core.models.UTCTime
import esw.ocs.dsl.SuspendableCallback
import esw.ocs.dsl.SuspendableConsumer
import esw.ocs.dsl.jdk.SuspendToJavaConverter
import esw.ocs.dsl.onFailedTerminate
import esw.ocs.dsl.script.utils.CommandUtil
import esw.ocs.dsl.script.utils.LockUnlockUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import msocket.api.Subscription
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class RichComponent(
        val prefix: Prefix,
        val componentType: ComponentType,
        private val lockUnlockUtil: LockUnlockUtil,
        private val commandUtil: CommandUtil,
        private val actorSystem: ActorSystem<*>,
        private val defaultTimeout: Duration,
        override val coroutineScope: CoroutineScope
) : SuspendToJavaConverter {

    /**
     * Sends validate command to component. Returns the ValidateResponse can be of type Accepted, Invalid
     * or Locked.
     *
     * @param command the [[csw.params.commands.ControlCommand]] payload
     * @return ValidateResponse [[csw.params.commands.CommandResponse.ValidateResponse]]
     */
    suspend fun validate(command: ControlCommand): ValidateResponse = commandService().validate(command).await()

    /**
     * Send a command as a Oneway and get a [[csw.params.commands.CommandResponse.OnewayResponse]]. The CommandResponse can be a response
     * of validation (Accepted, Invalid), or a Locked response.
     *
     * @param command the [[csw.params.commands.ControlCommand]] payload
     * @return a OnewayResponse
     */
    suspend fun oneway(command: ControlCommand): OnewayResponse = commandService().oneway(command).await()

    /**
     * Submit a command to assembly/hcd and return after first phase. If it returns as `Started` get a
     * final [[csw.params.commands.CommandResponse.SubmitResponse]] as a Future with queryFinal.
     *
     * @param command the [[csw.params.commands.ControlCommand]] payload
     * @param resumeOnError script execution continues if set true. If false, script execution flow breaks and sequence in
     * execution completes with failure.
     * @return a CommandResponse as a Future value
     */
    suspend fun submit(command: ControlCommand, resumeOnError: Boolean = false): SubmitResponse =
            actionOnResponse(resumeOnError) { commandService().submit(command).await() }

    /**
     * Query for the result of a long running command which was sent as Submit to get a [[csw.params.commands.CommandResponse.SubmitResponse]]
     *
     * @param commandRunId the runId of the command for which response is required
     * @param resumeOnError script execution continues if set true. If false, script execution flow breaks and sequence in
     * execution completes with failure.
     * @return a CommandResponse
     */
    suspend fun query(commandRunId: Id, resumeOnError: Boolean = false): SubmitResponse =
            actionOnResponse(resumeOnError) { commandService().query(commandRunId).await() }

    /**
     * Query for the final result of a long running command which was sent as Submit to get a [[csw.params.commands.CommandResponse.SubmitResponse]]
     *
     * @param commandRunId the runId of the command for which response is required
     * @param timeout duration for which api will wait for final response, if command is not completed queryFinal will timeout
     * @param resumeOnError script execution continues if set true. If false, script execution flow breaks and sequence in
     * execution completes with failure.
     * @return a CommandResponse
     */
    suspend fun queryFinal(commandRunId: Id, timeout: Duration = defaultTimeout, resumeOnError: Boolean = false): SubmitResponse =
            actionOnResponse(resumeOnError) { commandService().queryFinal(commandRunId, timeout.toTimeout()).await() }

    /**
     * Submit a command and wait for the final result if it was successfully validated as `Started` to get a
     * final [[csw.params.commands.CommandResponse.SubmitResponse]]
     *
     * @param command the [[csw.params.commands.ControlCommand]] payload
     * @param timeout duration for which api will wait for final response, if command is not completed queryFinal will timeout
     * @param resumeOnError script execution continues if set true. If false, script execution flow breaks and sequence in
     * execution completes with failure.
     * @return a CommandResponse
     */
    suspend fun submitAndWait(command: ControlCommand, timeout: Duration = defaultTimeout, resumeOnError: Boolean = false): SubmitResponse =
            actionOnResponse(resumeOnError) { commandService().submitAndWait(command, timeout.toTimeout()).await() }

    /**
     * Subscribe to the current state of a component
     *
     * @param stateNames subscribe to only those states which have any of the provided value for name
     * @param callback the action to be applied on the CurrentState element received as a result of subscription
     * @return a Subscription to stop the subscription
     */
    suspend fun subscribeCurrentState(vararg stateNames: StateName, callback: SuspendableConsumer<CurrentState>): Subscription =
            commandService().subscribeCurrentState(stateNames.toSet()) { callback.toJava(it) }

    /**
     * Send component into a diagnostic data mode based on a hint at the specified startTime.
     *
     * @param startTime represents the time at which the diagnostic mode actions will take effect
     * @param hint represents supported diagnostic data mode for a component
     */
    suspend fun diagnosticMode(startTime: UTCTime, hint: String): Unit = componentRef().tell(DiagnosticDataMessage.DiagnosticMode(startTime, hint))

    /**
     * Send component into an operations mode
     */
    suspend fun operationsMode(): Unit = componentRef().tell(DiagnosticDataMessage.`OperationsMode$`.`MODULE$`)

    /**
     * Send component into online mode
     */
    suspend fun goOnline(): Unit = componentRef().tell(RunningMessage.Lifecycle(ToComponentLifecycleMessage.jGoOnline()))

    /**
     * Send component into offline mode
     */
    suspend fun goOffline(): Unit = componentRef().tell(RunningMessage.Lifecycle(ToComponentLifecycleMessage.jGoOffline()))

    /**
     * Lock component for specified duration. Returns [[csw.command.client.models.framework.LockingResponse.LockAcquired]]
     * or [[csw.command.client.models.framework.LockingResponse.AcquiringLockFailed]]
     * @param leaseDuration duration for which component needs to be locked
     * @param onLockAboutToExpire callback which will be executed when Lock is about to expire
     * @param onLockExpired callback which will be executed when Lock is about to expire
     * @return return LockingResponse
     */
    suspend fun lock(
            leaseDuration: Duration,
            onLockAboutToExpire: SuspendableCallback = {},
            onLockExpired: SuspendableCallback = {}
    ): LockingResponse =
            lockUnlockUtil.lock(
                    componentRef(),
                    leaseDuration.toJavaDuration(),
                    { onLockAboutToExpire.toJava() },
                    { onLockExpired.toJava() }
            ).await()

    /**
     * Unlocks component. Returns [[csw.command.client.models.framework.LockingResponse.LockReleased]]
     * or [[csw.command.client.models.framework.LockingResponse.LockAlreadyReleased]] or [[csw.command.client.models.framework.LockingResponse.ReleasingLockFailed]]
     *
     * @return LockingResponse
     */
    suspend fun unlock(): LockingResponse = lockUnlockUtil.unlock(componentRef()).await()

    private suspend fun actionOnResponse(resumeOnError: Boolean = false, block: suspend () -> SubmitResponse): SubmitResponse =
            if (!resumeOnError) block().onFailedTerminate()
            else block()

    private fun Duration.toTimeout(): Timeout = Timeout(inWholeNanoseconds, TimeUnit.NANOSECONDS)

    private suspend fun commandService(): ICommandService = CommandServiceFactory.jMake(commandUtil.jResolvePekkoLocation(prefix, componentType).await(), actorSystem)
    private suspend fun componentRef(): ActorRef<ComponentMessage> = commandUtil.jResolveComponentRef(prefix, componentType).await()
}

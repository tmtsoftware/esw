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
import csw.location.models.ComponentType
import csw.params.commands.CommandResponse.*
import csw.params.commands.ControlCommand
import csw.params.core.models.Id
import csw.params.core.models.Prefix
import csw.params.core.states.CurrentState
import csw.params.core.states.StateName
import csw.time.core.models.UTCTime
import esw.ocs.dsl.SuspendableCallback
import esw.ocs.dsl.SuspendableConsumer
import esw.ocs.dsl.jdk.SuspendToJavaConverter
import esw.ocs.dsl.script.utils.LockUnlockUtil
import esw.ocs.dsl.sequence_manager.LocationServiceUtil
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
        private val locationServiceUtil: LocationServiceUtil,
        private val actorSystem: ActorSystem<*>,
        override val coroutineScope: CoroutineScope
) : SuspendToJavaConverter {

    suspend fun validate(command: ControlCommand): ValidateResponse = commandService().validate(command).await()
    suspend fun oneway(command: ControlCommand): OnewayResponse = commandService().oneway(command).await()
    suspend fun submit(command: ControlCommand): SubmitResponse = commandService().submit(command).await()
    suspend fun query(commandRunId: Id): SubmitResponse = commandService().query(commandRunId).await()

    suspend fun queryFinal(commandRunId: Id, timeout: Duration): SubmitResponse {
        val akkaTimeout = Timeout(timeout.toLongNanoseconds(), TimeUnit.NANOSECONDS)
        return commandService().queryFinal(commandRunId, akkaTimeout).await()
    }

    suspend fun submitAndWait(command: ControlCommand, timeout: Duration, onError: (SuspendableConsumer<SubmitResponse>)? = null):
            SubmitResponse {
        val akkaTimeout = Timeout(timeout.toLongNanoseconds(), TimeUnit.NANOSECONDS)
        val submitResponse = commandService().submitAndWait(command, akkaTimeout).await()
        return handleResponse(submitResponse, onError)
    }

    suspend fun subscribeCurrentState(stateNames: Set<StateName>, callback: SuspendableConsumer<CurrentState>): Subscription =
            commandService().subscribeCurrentState(stateNames) { callback.toJava(it) }

    suspend fun diagnosticMode(startTime: UTCTime, hint: String): Unit = componentRef().tell(DiagnosticDataMessage.DiagnosticMode(startTime, hint))
    suspend fun operationsMode(): Unit = componentRef().tell(DiagnosticDataMessage.`OperationsMode$`.`MODULE$`)

    suspend fun goOnline(): Unit = componentRef().tell(RunningMessage.Lifecycle(ToComponentLifecycleMessage.jGoOnline()))
    suspend fun goOffline(): Unit = componentRef().tell(RunningMessage.Lifecycle(ToComponentLifecycleMessage.jGoOffline()))

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

    suspend fun unlock(): LockingResponse = lockUnlockUtil.unlock(componentRef()).await()

    private suspend fun commandService(): ICommandService = CommandServiceFactory.jMake(locationServiceUtil.jResolveAkkaLocation(prefix, componentType).await(), actorSystem)
    private suspend fun componentRef(): ActorRef<ComponentMessage> = locationServiceUtil.jResolveComponentRef(prefix, componentType).await()

    private suspend fun handleResponse(submitResponse: SubmitResponse, handler: SuspendableConsumer<SubmitResponse>?): SubmitResponse {
        if (isNegative(submitResponse))
            handler?.let { it(coroutineScope, submitResponse) } ?: throw SubmitError(submitResponse)
        return submitResponse
    }

}

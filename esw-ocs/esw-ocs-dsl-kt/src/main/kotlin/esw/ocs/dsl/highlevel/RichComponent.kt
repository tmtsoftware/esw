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
import csw.params.core.states.CurrentState
import csw.params.core.states.StateName
import csw.prefix.models.Prefix
import csw.time.core.models.UTCTime
import esw.ocs.dsl.SuspendableCallback
import esw.ocs.dsl.SuspendableConsumer
import esw.ocs.dsl.jdk.SuspendToJavaConverter
import esw.ocs.dsl.onFailedTerminate
import esw.ocs.dsl.script.utils.LockUnlockUtil
import esw.ocs.impl.internal.LocationServiceUtil
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
        private val defaultTimeout: Duration,
        override val coroutineScope: CoroutineScope
) : SuspendToJavaConverter {

    suspend fun validate(command: ControlCommand): ValidateResponse = commandService().validate(command).await()
    suspend fun oneway(command: ControlCommand): OnewayResponse = commandService().oneway(command).await()

    suspend fun submit(command: ControlCommand, resumeOnError: Boolean = false): SubmitResponse =
            actionOnResponse(resumeOnError) { commandService().submit(command).await() }

    suspend fun query(commandRunId: Id, resumeOnError: Boolean = false): SubmitResponse =
            actionOnResponse(resumeOnError) { commandService().query(commandRunId).await() }

    suspend fun queryFinal(commandRunId: Id, timeout: Duration = defaultTimeout, resumeOnError: Boolean = false): SubmitResponse =
            actionOnResponse(resumeOnError) { commandService().queryFinal(commandRunId, timeout.toTimeout()).await() }

    suspend fun submitAndWait(command: ControlCommand, timeout: Duration = defaultTimeout, resumeOnError: Boolean = false): SubmitResponse =
            actionOnResponse(resumeOnError) { commandService().submitAndWait(command, timeout.toTimeout()).await() }

    suspend fun subscribeCurrentState(vararg stateNames: StateName, callback: SuspendableConsumer<CurrentState>): Subscription =
            commandService().subscribeCurrentState(stateNames.toSet()) { callback.toJava(it) }

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

    private suspend fun actionOnResponse(resumeOnError: Boolean = false, block: suspend () -> SubmitResponse): SubmitResponse =
            if (!resumeOnError) block().onFailedTerminate()
            else block()

    private fun Duration.toTimeout(): Timeout = Timeout(toLongNanoseconds(), TimeUnit.NANOSECONDS)

    private suspend fun commandService(): ICommandService = CommandServiceFactory.jMake(locationServiceUtil.jResolveAkkaLocation(prefix, componentType).await(), actorSystem)
    private suspend fun componentRef(): ActorRef<ComponentMessage> = locationServiceUtil.jResolveComponentRef(prefix, componentType).await()
}

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
import csw.params.core.models.Prefix
import csw.params.core.states.CurrentState
import csw.params.core.states.StateName
import csw.time.core.models.UTCTime
import esw.ocs.dsl.Timeouts
import esw.ocs.dsl.jdk.SuspendToJavaConverter
import esw.ocs.dsl.script.utils.LockUnlockUtil
import esw.ocs.dsl.sequence_manager.LocationServiceUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import msocket.api.models.Subscription
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class RichComponent(
        val name: String,
        val componentType: ComponentType,
        private val prefix: Prefix,
        private val lockUnlockUtil: LockUnlockUtil,
        private val locationServiceUtil: LocationServiceUtil,
        private val actorSystem: ActorSystem<*>,
        override val coroutineScope: CoroutineScope
) : SuspendToJavaConverter {
    private val timeout: Timeout = Timeout(Timeouts.DefaultTimeout())

    suspend fun validate(command: ControlCommand): ValidateResponse = commandService().validate(command).await()
    suspend fun oneway(command: ControlCommand): OnewayResponse = commandService().oneway(command, timeout).await()
    suspend fun submit(command: ControlCommand): SubmitResponse = commandService().submit(command, timeout).await()
    suspend fun submitAndWait(command: ControlCommand): SubmitResponse = commandService().submitAndWait(command, timeout).await()
    suspend fun subscribeCurrentState(stateNames: Set<StateName>, callback: suspend CoroutineScope.(CurrentState) -> Unit): Subscription =
            commandService().subscribeCurrentState(stateNames) { callback.toJava(it) }

    suspend fun diagnosticMode(startTime: UTCTime, hint: String): Unit = componentRef().tell(DiagnosticDataMessage.DiagnosticMode(startTime, hint))
    suspend fun operationsMode(): Unit = componentRef().tell(DiagnosticDataMessage.`OperationsMode$`.`MODULE$`)

    suspend fun goOnline(): Unit = componentRef().tell(RunningMessage.Lifecycle(ToComponentLifecycleMessage.jGoOnline()))
    suspend fun goOffline(): Unit = componentRef().tell(RunningMessage.Lifecycle(ToComponentLifecycleMessage.jGoOffline()))

    suspend fun lock(
            leaseDuration: Duration,
            onLockAboutToExpire: suspend CoroutineScope.() -> Unit = {},
            onLockExpired: suspend CoroutineScope.() -> Unit = {}
    ): LockingResponse =
            lockUnlockUtil.lock(
                    componentRef(),
                    prefix,
                    leaseDuration.toJavaDuration(),
                    { onLockAboutToExpire.toJava() },
                    { onLockExpired.toJava() }
            ).await()

    suspend fun unlock(): LockingResponse = lockUnlockUtil.unlock(componentRef(), prefix).await()

    private suspend fun commandService(): ICommandService = CommandServiceFactory.jMake(locationServiceUtil.jResolveAkkaLocation(name, componentType).await(), actorSystem)
    private suspend fun componentRef(): ActorRef<ComponentMessage> = locationServiceUtil.jResolveComponentRef(name, componentType).await()
}

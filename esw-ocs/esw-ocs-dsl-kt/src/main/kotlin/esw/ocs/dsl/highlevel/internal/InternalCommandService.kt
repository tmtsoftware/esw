package esw.ocs.dsl.highlevel.internal

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
import csw.location.models.ComponentType
import csw.params.commands.CommandResponse.*
import csw.params.commands.ControlCommand
import csw.params.core.models.Prefix
import csw.time.core.models.UTCTime
import esw.ocs.dsl.highlevel.JavaFutureInterop
import esw.ocs.dsl.script.utils.LockUnlockUtil
import esw.ocs.dsl.sequence_manager.LocationServiceUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class InternalCommandService(
        private val name: String,
        private val compType: ComponentType,
        private val lockUnlockUtil: LockUnlockUtil,
        private val locationServiceUtil: LocationServiceUtil,
        private val actorSystem: ActorSystem<*>,
        private val timeout: Timeout,
        override val coroutineScope: CoroutineScope
) : JavaFutureInterop {

    suspend fun validate(command: ControlCommand): ValidateResponse = commandService().validate(command).await()
    suspend fun oneway(command: ControlCommand): OnewayResponse = commandService().oneway(command, timeout).await()
    suspend fun submit(command: ControlCommand): SubmitResponse = commandService().submit(command, timeout).await()
    suspend fun submitAndWait(command: ControlCommand): SubmitResponse = commandService().submitAndWait(command, timeout).await()

    suspend fun diagnosticMode(startTime: UTCTime, hint: String): Unit = componentRef().tell(DiagnosticDataMessage.DiagnosticMode(startTime, hint))
    suspend fun operationsMode(): Unit = componentRef().tell(DiagnosticDataMessage.`OperationsMode$`.`MODULE$`)

    suspend fun goOnline(): Unit = componentRef().tell(RunningMessage.Lifecycle(ToComponentLifecycleMessage.`GoOnline$`.`MODULE$`))
    suspend fun goOffline(): Unit = componentRef().tell(RunningMessage.Lifecycle(ToComponentLifecycleMessage.`GoOffline$`.`MODULE$`))

    suspend fun lock(
            prefix: String,
            leaseDuration: Duration,
            onLockAboutToExpire: suspend CoroutineScope.() -> Unit,
            onLockExpired: suspend CoroutineScope.() -> Unit
    ): LockingResponse =
            lockUnlockUtil.lock(
                    componentRef(),
                    Prefix(prefix),
                    leaseDuration.toJavaDuration(),
                    { onLockAboutToExpire.toJavaFutureVoid() },
                    { onLockExpired.toJavaFutureVoid() }
            ).await()

    suspend fun unlock(prefix: String): LockingResponse = lockUnlockUtil.unlock(componentRef(), Prefix(prefix)).await()

    private suspend fun commandService(): ICommandService = CommandServiceFactory.jMake(locationServiceUtil.jResolveAkkaLocation(name, compType).await(), actorSystem)
    private suspend fun componentRef(): ActorRef<ComponentMessage> = locationServiceUtil.jResolveComponentRef(name, JComponentType.Assembly()).await()
}

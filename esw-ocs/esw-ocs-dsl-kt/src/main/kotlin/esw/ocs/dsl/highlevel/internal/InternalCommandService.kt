package esw.ocs.dsl.highlevel.internal

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.util.Timeout
import csw.command.api.javadsl.ICommandService
import csw.command.client.CommandServiceFactory
import csw.command.client.messages.ComponentMessage
import csw.command.client.models.framework.LockingResponse
import csw.location.api.javadsl.JComponentType
import csw.location.models.ComponentType
import csw.params.commands.CommandResponse.*
import csw.params.commands.ControlCommand
import csw.time.core.models.UTCTime
import esw.ocs.dsl.highlevel.CommandServiceUtil
import esw.ocs.dsl.sequence_manager.LocationServiceUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlin.time.Duration

class InternalCommandService(
        private val name: String,
        private val compType: ComponentType,
        private val commandServiceUtil: CommandServiceUtil,
        private val locationServiceUtil: LocationServiceUtil,
        private val actorSystem: ActorSystem<*>,
        private val timeout: Timeout
) {

    private suspend fun commandService(): ICommandService = CommandServiceFactory.jMake(locationServiceUtil.jResolveAkkaLocation(name, compType).await(), actorSystem)
    private suspend fun componentRef(): ActorRef<ComponentMessage> = locationServiceUtil.jResolveComponentRef(name, JComponentType.Assembly()).await()

    suspend fun validate(command: ControlCommand): ValidateResponse = commandService().validate(command).await()
    suspend fun oneway(command: ControlCommand): OnewayResponse = commandService().oneway(command, timeout).await()
    suspend fun submit(command: ControlCommand): SubmitResponse = commandService().submit(command, timeout).await()
    suspend fun submitAndWait(command: ControlCommand): SubmitResponse = commandService().submitAndWait(command, timeout).await()

    suspend fun diagnosticMode(startTime: UTCTime, hint: String): Unit = commandServiceUtil.diagnosticMode(componentRef(), startTime, hint)
    suspend fun operationsMode(): Unit = commandServiceUtil.operationsMode(componentRef())

    suspend fun goOnline(): Unit = commandServiceUtil.goOnline(componentRef())
    suspend fun goOffline(): Unit = commandServiceUtil.goOffline(componentRef())

    suspend fun lock(
            prefix: String,
            leaseDuration: Duration,
            onLockAboutToExpire: suspend CoroutineScope.() -> Unit,
            onLockExpired: suspend CoroutineScope.() -> Unit
    ): LockingResponse = commandServiceUtil.lock(componentRef(), prefix, leaseDuration, onLockAboutToExpire, onLockExpired)

    suspend fun unlock(prefix: String): LockingResponse = commandServiceUtil.unlock(componentRef(), prefix)

}

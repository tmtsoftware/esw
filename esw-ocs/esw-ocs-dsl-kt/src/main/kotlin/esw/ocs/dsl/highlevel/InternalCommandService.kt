package esw.ocs.dsl.highlevel

import akka.actor.typed.ActorRef
import akka.util.Timeout
import csw.command.api.javadsl.ICommandService
import csw.command.client.messages.ComponentMessage
import csw.command.client.messages.DiagnosticDataMessage
import csw.command.client.messages.RunningMessage
import csw.command.client.models.framework.LockingResponse
import csw.command.client.models.framework.ToComponentLifecycleMessage
import csw.params.commands.CommandResponse.*
import csw.params.commands.ControlCommand
import csw.params.core.models.Prefix
import csw.time.core.models.UTCTime
import esw.ocs.dsl.script.utils.LockUnlockUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class InternalCommandService(private val commandService: ICommandService, private val commandServiceUtil: CommandServiceUtil,private val timeout: Timeout) {

    suspend fun validate(command: ControlCommand): ValidateResponse = commandService.validate(command).await()
    suspend fun oneway(command: ControlCommand): OnewayResponse = commandService.oneway(command, timeout).await()
    suspend fun submit(command: ControlCommand): SubmitResponse = commandService.submit(command, timeout).await()
    suspend fun submitAndWait(command: ControlCommand): SubmitResponse = commandService.submitAndWait(command, timeout).await()

    fun diagnosticMode(startTime: UTCTime, hint: String): Unit = commandServiceUtil.diagnosticMode(startTime, hint)
    fun operationsMode(): Unit = commandServiceUtil.operationsMode()

    fun goOnline(): Unit = commandServiceUtil.goOnline()
    fun goOffline(): Unit = commandServiceUtil.goOffline()

    suspend fun lock(
            prefix: String,
            leaseDuration: Duration,
            onLockAboutToExpire: suspend CoroutineScope.() -> Unit,
            onLockExpired: suspend CoroutineScope.() -> Unit
    ): LockingResponse = commandServiceUtil.lock(prefix, leaseDuration, onLockAboutToExpire, onLockExpired)

    suspend fun unlock(prefix: String): LockingResponse = commandServiceUtil.unlock(prefix)

}

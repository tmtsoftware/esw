package esw.ocs.dsl.highlevel

import akka.actor.typed.ActorRef
import csw.command.client.messages.ComponentMessage
import csw.command.client.messages.DiagnosticDataMessage
import csw.command.client.messages.RunningMessage.Lifecycle
import csw.command.client.models.framework.LockingResponse
import csw.command.client.models.framework.ToComponentLifecycleMessage
import csw.params.core.models.Prefix
import csw.time.core.models.UTCTime
import esw.ocs.dsl.script.utils.LockUnlockUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class CommandServiceUtil(private val lockUnlockUtil: LockUnlockUtil, override val coroutineScope: CoroutineScope) : JavaFutureInterop {

    /**
     * @param prefix prefix of component that is acquiring lock
     * @param leaseDuration duration for which lock is getting acquired
     * @param onLockAboutToExpire callback that gets called on receiving [csw.command.client.models.framework.LockingResponse.LockExpiringShortly] message
     * @param onLockExpired callback that gets called on receiving [csw.command.client.models.framework.LockingResponse.LockExpired] message
     * @return initial lock response that can be one of
     * — [csw.command.client.models.framework.LockingResponse.LockAcquired]
     * — [csw.command.client.models.framework.LockingResponse.AcquiringLockFailed]
     */
    suspend fun lock(
            componentRef: ActorRef<ComponentMessage>,
            prefix: String,
            leaseDuration: Duration,
            onLockAboutToExpire: suspend CoroutineScope.() -> Unit,
            onLockExpired: suspend CoroutineScope.() -> Unit
    ): LockingResponse =
            lockUnlockUtil.lock(
                    componentRef,
                    Prefix(prefix),
                    leaseDuration.toJavaDuration(),
                    { onLockAboutToExpire.toJavaFutureVoid() },
                    { onLockExpired.toJavaFutureVoid() }
            ).await()

    /**
     * @param prefix prefix of component that has acquired lock previously
     * @return lock release response either successful or failure
     */
    suspend fun unlock(componentRef: ActorRef<ComponentMessage>, prefix: String): LockingResponse =
            lockUnlockUtil.unlock(componentRef, Prefix(prefix)).await()

    fun diagnosticMode(componentRef: ActorRef<ComponentMessage>, startTime: UTCTime, hint: String): Unit =
            componentRef.tell(DiagnosticDataMessage.DiagnosticMode(startTime, hint))

    fun operationsMode(componentRef: ActorRef<ComponentMessage>): Unit = componentRef.tell(DiagnosticDataMessage.`OperationsMode$`.`MODULE$`)

    fun goOnline(componentRef: ActorRef<ComponentMessage>): Unit =
            componentRef.tell(Lifecycle(ToComponentLifecycleMessage.`GoOnline$`.`MODULE$`))

    fun goOffline(componentRef: ActorRef<ComponentMessage>): Unit =
            componentRef.tell(Lifecycle(ToComponentLifecycleMessage.`GoOffline$`.`MODULE$`))

}
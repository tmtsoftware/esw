//package esw.ocs.dsl.highlevel
//
//import csw.command.client.models.framework.LockingResponse
//import csw.location.api.javadsl.JComponentType.Assembly
//import csw.location.api.javadsl.JComponentType.HCD
//import csw.params.core.models.Prefix
//import esw.ocs.dsl.script.utils.LockUnlockUtil
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.future.await
//import kotlin.time.Duration
//import kotlin.time.toJavaDuration
//
//interface LockUnlockDsl : JavaFutureInterop {
//    val lockUnlockUtil: LockUnlockUtil
//
//    /************* Assembly *************/
//    /**
//     * @param assemblyName name of assembly to be locked
//     * @param prefix prefix of component that is acquiring lock
//     * @param leaseDuration duration for which lock is getting acquired
//     * @param onLockAboutToExpire callback that gets called on receiving [csw.command.client.models.framework.LockingResponse.LockExpiringShortly] message
//     * @param onLockExpired callback that gets called on receiving [csw.command.client.models.framework.LockingResponse.LockExpired] message
//     * @return initial lock response that can be one of
//     * — [csw.command.client.models.framework.LockingResponse.LockAcquired]
//     * — [csw.command.client.models.framework.LockingResponse.AcquiringLockFailed]
//     */
//    suspend fun lockAssembly(
//            assemblyName: String,
//            prefix: String,
//            leaseDuration: Duration,
//            onLockAboutToExpire: suspend CoroutineScope.() -> Unit,
//            onLockExpired: suspend CoroutineScope.() -> Unit
//    ): LockingResponse =
//            lockUnlockUtil.lock(assemblyName, Assembly(), Prefix(prefix), leaseDuration.toJavaDuration(), { onLockAboutToExpire.toJavaFutureVoid() }, { onLockExpired.toJavaFutureVoid() }).await()
//
//    /**
//     * @param assemblyName name of assembly to be unlocked
//     * @param prefix prefix of component that has acquired lock previously
//     * @return lock release response either successful or failure
//     */
//    suspend fun unlockAssembly(assemblyName: String, prefix: String): LockingResponse =
//            lockUnlockUtil.unlock(assemblyName, Assembly(), Prefix(prefix)).await()
//
//    /************* HCD *************/
//    /**
//     * @param hcdName name of hcd to be locked
//     * @param prefix prefix of component that is acquiring lock
//     * @param leaseDuration duration for which lock is getting acquired
//     * @param onLockAboutToExpire callback that gets called on receiving [csw.command.client.models.framework.LockingResponse.LockExpiringShortly] message
//     * @param onLockExpired callback that gets called on receiving [csw.command.client.models.framework.LockingResponse.LockExpired] message
//     * @return initial lock response that can be one of
//     * — [csw.command.client.models.framework.LockingResponse.LockAcquired]
//     * — [csw.command.client.models.framework.LockingResponse.AcquiringLockFailed]
//     */
//    suspend fun lockHcd(
//            hcdName: String,
//            prefix: String,
//            leaseDuration: Duration,
//            onLockAboutToExpire: suspend CoroutineScope.() -> Unit,
//            onLockExpired: suspend CoroutineScope.() -> Unit
//    ): LockingResponse =
//            lockUnlockUtil.lock(hcdName, HCD(), Prefix(prefix), leaseDuration.toJavaDuration(), { onLockAboutToExpire.toJavaFutureVoid() }, { onLockExpired.toJavaFutureVoid() }).await()
//
//    /**
//     * @param hcdName name of hcd to be unlocked
//     * @param prefix prefix of component that has acquired lock previously
//     * @return lock release response either successful or failure
//     */
//    suspend fun unlockHcd(hcdName: String, prefix: String): LockingResponse =
//            lockUnlockUtil.unlock(hcdName, HCD(), Prefix(prefix)).await()
//
//}

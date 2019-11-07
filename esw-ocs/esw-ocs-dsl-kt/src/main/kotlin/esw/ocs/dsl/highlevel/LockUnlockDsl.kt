package esw.ocs.dsl.highlevel

import csw.command.client.models.framework.LockingResponse
import csw.location.api.javadsl.JComponentType.Assembly
import csw.location.api.javadsl.JComponentType.HCD
import csw.params.core.models.Prefix
import esw.ocs.dsl.script.utils.LockUnlockUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlin.time.Duration
import kotlin.time.toJavaDuration

interface LockUnlockDsl : JavaFutureInterop {
    val lockUnlockUtil: LockUnlockUtil

    /************* Assembly *************/
    fun lockAssembly(
            assemblyName: String,
            prefix: Prefix,
            leaseDuration: Duration,
            callback: suspend CoroutineScope.(LockingResponse) -> Unit
    ) {
        lockUnlockUtil.lock(assemblyName, Assembly(), prefix, leaseDuration.toJavaDuration()) { callback.toJavaFuture(it) }
    }

    suspend fun unlockAssembly(assemblyName: String, prefix: Prefix): LockingResponse =
            lockUnlockUtil.unlock(assemblyName, Assembly(), prefix).await()

    /************* HCD *************/
    fun lockHcd(hcdName: String, prefix: Prefix, leaseDuration: Duration, callback: suspend CoroutineScope.(LockingResponse) -> Unit) {
        lockUnlockUtil.lock(hcdName, HCD(), prefix, leaseDuration.toJavaDuration()) { callback.toJavaFuture(it) }
    }

    suspend fun unlockHcd(hcdName: String, prefix: Prefix): LockingResponse =
            lockUnlockUtil.unlock(hcdName, HCD(), prefix).await()

}

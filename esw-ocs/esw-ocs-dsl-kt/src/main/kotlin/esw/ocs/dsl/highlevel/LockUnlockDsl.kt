package esw.ocs.dsl.highlevel

import csw.command.client.models.framework.LockingResponse
import csw.location.api.javadsl.JComponentType.Assembly
import csw.location.api.javadsl.JComponentType.HCD
import csw.params.core.models.Prefix
import esw.ocs.dsl.script.utils.LockUnlockUtil
import java.time.Duration
import kotlinx.coroutines.future.await

interface LockUnlockDsl {
    val lockUnlockUtil: LockUnlockUtil

    /************* Assembly *************/
    suspend fun lockAssembly(assemblyName: String, prefix: Prefix, leaseDuration: Duration): LockingResponse =
        lockUnlockUtil.jLock(assemblyName, Assembly, prefix, leaseDuration).await()

    suspend fun unlockAssembly(assemblyName: String, prefix: Prefix): LockingResponse =
        lockUnlockUtil.jUnlock(assemblyName, Assembly, prefix).await()

    /************* HCD *************/
    suspend fun lockHcd(hcdName: String, prefix: Prefix, leaseDuration: Duration): LockingResponse =
        lockUnlockUtil.jLock(hcdName, HCD, prefix, leaseDuration).await()

    suspend fun unlockHcd(hcdName: String, prefix: Prefix): LockingResponse =
        lockUnlockUtil.jUnlock(hcdName, HCD, prefix).await()
}

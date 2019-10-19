package esw.ocs.dsl.highlevel

import csw.command.client.models.framework.LockingResponse
import csw.location.api.javadsl.JComponentType.Assembly
import csw.location.api.javadsl.JComponentType.HCD
import csw.params.core.models.Prefix
import esw.ocs.dsl.script.utils.LockUnlockUtil
import io.kotlintest.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration
import kotlin.time.seconds
import kotlin.time.toJavaDuration

class LockUnlockDslTest : LockUnlockDsl {
    private val componentName = "test-component"
    private val assembly = Assembly()
    private val hcd = HCD()
    private val prefix = Prefix("esw")
    private val leaseDuration: Duration = 10.seconds
    private val jLeaseDuration = leaseDuration.toJavaDuration()

    private val mockLockUnlockUtil = mockk<LockUnlockUtil>()
    private val lockingResponse = mockk<LockingResponse>()

    override val lockUnlockUtil: LockUnlockUtil = mockLockUnlockUtil

    @Test
    fun `LockUnlockDsl should lockAssembly should delegate to LockUnlockUtil#jLock | ESW-126`() = runBlocking {
        every {
            mockLockUnlockUtil.jLock(componentName, assembly, prefix, jLeaseDuration)
        }.returns(CompletableFuture.completedFuture(lockingResponse))

        lockAssembly(componentName, prefix, jLeaseDuration) shouldBe lockingResponse
        verify { lockUnlockUtil.jLock(componentName, assembly, prefix, jLeaseDuration) }
    }

    @Test
    fun `unlockAssembly should delegate to LockUnlockUtil#jUnlock | ESW-126`() = runBlocking {
        every {
            mockLockUnlockUtil.jUnlock(componentName, assembly, prefix)
        }.returns(CompletableFuture.completedFuture(lockingResponse))

        unlockAssembly(componentName, prefix) shouldBe lockingResponse
        verify { lockUnlockUtil.jUnlock(componentName, assembly, prefix) }
    }

    @Test
    fun `lockHcd should delegate to LockUnlockUtil#jLock | ESW-126`() = runBlocking {
        every {
            mockLockUnlockUtil.jLock(componentName, hcd, prefix, jLeaseDuration)
        }.returns(CompletableFuture.completedFuture(lockingResponse))

        lockHcd(componentName, prefix, jLeaseDuration) shouldBe lockingResponse
        verify { lockUnlockUtil.jLock(componentName, hcd, prefix, jLeaseDuration) }
    }

    @Test
    fun `unlockHcd should delegate to LockUnlockUtil#jUnlock | ESW-126`() = runBlocking {
        every {
            mockLockUnlockUtil.jUnlock(componentName, hcd, prefix)
        }.returns(CompletableFuture.completedFuture(lockingResponse))

        unlockHcd(componentName, prefix) shouldBe lockingResponse
        verify { lockUnlockUtil.jUnlock(componentName, hcd, prefix) }
    }
}

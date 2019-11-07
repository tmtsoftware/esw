package esw.ocs.dsl.highlevel

import akka.Done
import csw.command.client.models.framework.LockingResponse
import csw.location.api.javadsl.JComponentType.Assembly
import csw.location.api.javadsl.JComponentType.HCD
import csw.params.core.models.Prefix
import esw.ocs.dsl.script.utils.LockUnlockUtil
import io.kotlintest.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.seconds
import kotlin.time.toJavaDuration

class LockUnlockDslTest : LockUnlockDsl {
    private val componentName = "test-component"
    private val assembly = Assembly()
    private val hcd = HCD()
    private val prefix = Prefix("esw")
    private val leaseDuration: Duration = 10.seconds
    private val jLeaseDuration: java.time.Duration = leaseDuration.toJavaDuration()

    private val mockLockUnlockUtil = mockk<LockUnlockUtil>()
    private val lockingResponse = mockk<LockingResponse>()

    override val lockUnlockUtil: LockUnlockUtil = mockLockUnlockUtil
    override val coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)

    @Test
    fun `LockUnlockDsl should lockAssembly should delegate to LockUnlockUtil#lock | ESW-126`() = runBlocking {

        every {
            mockLockUnlockUtil.lock(componentName, assembly, prefix, jLeaseDuration, any())
        }.returns(CompletableFuture.completedFuture(Done.done()))

        lockAssembly(componentName, prefix, leaseDuration) {}
        verify { lockUnlockUtil.lock(componentName, assembly, prefix, jLeaseDuration, any()) }
    }

    @Test
    fun `unlockAssembly should delegate to LockUnlockUtil#unlock | ESW-126`() = runBlocking {
        every {
            mockLockUnlockUtil.unlock(componentName, assembly, prefix)
        }.returns(CompletableFuture.completedFuture(lockingResponse))

        unlockAssembly(componentName, prefix) shouldBe lockingResponse
        verify { lockUnlockUtil.unlock(componentName, assembly, prefix) }
    }

    @Test
    fun `lockHcd should delegate to LockUnlockUtil#lock | ESW-126`() = runBlocking {
        every {
            mockLockUnlockUtil.lock(componentName, hcd, prefix, jLeaseDuration, any())
        }.returns(CompletableFuture.completedFuture(Done.done()))

        lockHcd(componentName, prefix, leaseDuration) {}
        verify { lockUnlockUtil.lock(componentName, hcd, prefix, jLeaseDuration, any()) }
    }

    @Test
    fun `unlockHcd should delegate to LockUnlockUtil#unlock | ESW-126`() = runBlocking {
        every {
            mockLockUnlockUtil.unlock(componentName, hcd, prefix)
        }.returns(CompletableFuture.completedFuture(lockingResponse))

        unlockHcd(componentName, prefix) shouldBe lockingResponse
        verify { lockUnlockUtil.unlock(componentName, hcd, prefix) }
    }
}

package esw.ocs.dsl.highlevel

import csw.command.client.models.framework.LockingResponse
import csw.location.api.javadsl.JComponentType.Assembly
import csw.location.api.javadsl.JComponentType.HCD
import csw.params.core.models.Prefix
import esw.ocs.dsl.script.utils.LockUnlockUtil
import io.kotlintest.specs.WordSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration
import kotlin.time.seconds
import kotlin.time.toJavaDuration

class LockUnlockDslTest : WordSpec({
    class Mocks {
        val mockLockUnlockUtil = mockk<LockUnlockUtil>()
        val componentName = "test-component"
        val assembly = Assembly
        val hcd = HCD
        val prefix = Prefix("esw")
        val leaseDuration: Duration = 10.seconds
        val jLeaseDuration = leaseDuration.toJavaDuration()
        val lockingResponse = mockk<LockingResponse>()

        val lockUnlockDsl = object : LockUnlockDsl {
            override val lockUnlockUtil: LockUnlockUtil = mockLockUnlockUtil
        }
    }

    "LockUnlockDsl" should {
        "lockAssembly should delegate to LockUnlockUtil.jLock | ESW-126" {
            with(Mocks()) {
                every { mockLockUnlockUtil.jLock(componentName, assembly, prefix, jLeaseDuration) }.returns(
                    CompletableFuture.completedFuture(lockingResponse)
                )

                lockUnlockDsl.lockAssembly(componentName, prefix, jLeaseDuration)

                verify { lockUnlockDsl.lockUnlockUtil.jLock(componentName, assembly, prefix, jLeaseDuration) }
            }
        }

        "unlockAssembly should delegate to LockUnlockUtil.jUnlock | ESW-126" {
            with(Mocks()) {
                every {
                    mockLockUnlockUtil.jUnlock(
                        componentName,
                        assembly,
                        prefix
                    )
                }.returns(CompletableFuture.completedFuture(lockingResponse))

                lockUnlockDsl.unlockAssembly(componentName, prefix)

                verify { lockUnlockDsl.lockUnlockUtil.jUnlock(componentName, assembly, prefix) }
            }
        }

        "lockHcd should delegate to LockUnlockUtil.jLock | ESW-126" {
            with(Mocks()) {
                every {
                    mockLockUnlockUtil.jLock(
                        componentName,
                        hcd,
                        prefix,
                        jLeaseDuration
                    )
                }.returns(CompletableFuture.completedFuture(lockingResponse))

                lockUnlockDsl.lockHcd(componentName, prefix, jLeaseDuration)

                verify { lockUnlockDsl.lockUnlockUtil.jLock(componentName, hcd, prefix, jLeaseDuration) }
            }
        }

        "unlockHcd should delegate to LockUnlockUtil.jUnlock | ESW-126" {
            with(Mocks()) {
                every {
                    mockLockUnlockUtil.jUnlock(
                        componentName,
                        hcd,
                        prefix
                    )
                }.returns(CompletableFuture.completedFuture(lockingResponse))

                lockUnlockDsl.unlockHcd(componentName, prefix)

                verify { lockUnlockDsl.lockUnlockUtil.jUnlock(componentName, hcd, prefix) }
            }
        }
    }
})

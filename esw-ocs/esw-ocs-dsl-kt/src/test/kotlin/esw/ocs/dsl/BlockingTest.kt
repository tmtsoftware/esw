package esw.ocs.dsl

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEqualIgnoringCase
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

@Suppress("DANGEROUS_CHARACTERS")
class BlockingTest {

    @Test
    fun `blockingCpu_should_use_different_thread_than_main_thread_and_should_shutdown_underlying_thread_pool_|_ESW-184`() = runBlocking {
        lateinit var cpuThread: String

        val mainThread = Thread.currentThread().name
        blockingCpu {
            cpuThread = Thread.currentThread().name
        }

        cpuThread shouldNotBeEqualIgnoringCase mainThread

        // verify thread pool gets shutdown properly
        isCpuBoundDispatcherShutdown() shouldBe false
        shutdownCpuBoundDispatcher()
        isCpuBoundDispatcherShutdown() shouldBe true
    }

    @Test
    fun `blockingIo_should_use_different_thread_than_main_thread_|_ESW-184`() = runBlocking {
        lateinit var ioThread: String

        val mainThread = Thread.currentThread().name
        blockingIo {
            ioThread = Thread.currentThread().name
        }

        ioThread shouldNotBeEqualIgnoringCase mainThread
    }
}

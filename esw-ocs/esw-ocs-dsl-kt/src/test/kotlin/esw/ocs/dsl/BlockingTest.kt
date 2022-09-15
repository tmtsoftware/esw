/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.dsl

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEqualIgnoringCase
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

@Suppress("DANGEROUS_CHARACTERS")
class BlockingTest {

    @Test
    fun `blockingCpu should use different thread than main thread and should shutdown underlying thread pool | ESW-184`() = runBlocking {
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
    fun `blockingIo should use different thread than main thread | ESW-184`() = runBlocking {
        lateinit var ioThread: String

        val mainThread = Thread.currentThread().name
        blockingIo {
            ioThread = Thread.currentThread().name
        }

        ioThread shouldNotBeEqualIgnoringCase mainThread
    }
}

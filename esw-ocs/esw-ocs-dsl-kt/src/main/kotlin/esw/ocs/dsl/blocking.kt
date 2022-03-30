/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.dsl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Use this utility to run CPU bound/intensive operations.
 *
 * Calls the specified suspending block on a CPU bound thread pool which is backed by 4 threads,
 * suspends until it completes, and returns the result.
 *
 * @warning This is not thread safe API, dont mutate state in the provided callback
 */
suspend fun <T> blockingCpu(block: CoroutineScope.() -> T): T = withContext(CpuBound.value) { block() }

/**
 * Use this utility to run blocking IO bound operations.
 *
 * Calls the specified suspending block on a IO bound thread pool which is backed by 64 threads
 * or the number of cores (whichever is larger), suspends until it completes, and returns the result.
 *
 * @warning This is not thread safe API, dont mutate state in the provided callback
 */
suspend fun <T> blockingIo(block: CoroutineScope.() -> T): T = withContext(Dispatchers.IO) { block() }

// =========== INTERNAL ===========
private val cpuThreadPool: ExecutorService by lazy { Executors.newFixedThreadPool(4) }
private val CpuBound = lazy { cpuThreadPool.asCoroutineDispatcher() }

fun shutdownCpuBoundDispatcher() {
    if (CpuBound.isInitialized()) CpuBound.value.close()
}

fun isCpuBoundDispatcherShutdown() = cpuThreadPool.isShutdown


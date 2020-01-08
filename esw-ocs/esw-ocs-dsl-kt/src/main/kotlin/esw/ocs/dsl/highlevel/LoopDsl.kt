package esw.ocs.dsl.highlevel

import kotlinx.coroutines.*
import kotlin.time.Duration
import kotlin.time.milliseconds

interface LoopDsl {
    companion object {
        private val loopInterval: Duration = 50.milliseconds
    }

    val coroutineScope: CoroutineScope

    /*============== Synchronous loops ==============*/
    /**
     * Runs a loop synchronously
     * @param block lambda to be executed on every iteration of loop until `stopWhen(condition)` written inside lambda becomes true
     * Note: loop uses default loopInterval of `50 millis`
     **/
    suspend fun loop(block: suspend StopWhen.() -> Unit): Job = loop(loopInterval, block)

    /**
     * Runs a loop synchronously
     * @param minInterval every iteration of loop at least waits for provided minInterval before executing block of code
     * @param block lambda to be executed on every iteration of loop until `stopWhen(condition)` written inside lambda becomes true
     * Note: minInterval should be greater than `50 millis` otherwise default interval of 50 millis will be considered
     */
    suspend fun loop(minInterval: Duration, block: suspend StopWhen.() -> Unit): Job = loop0(minInterval, block)

    /**
     * This is a top-level construct which can be used when you want to wait for some condition to become true
     * @param condition lambda that returns boolean which gets evaluated every 50 milliseconds
     */
    suspend fun waitFor(condition: suspend () -> Boolean) = loop { stopWhen(condition()) }

    /*============== Asynchronous loops ==============*/
    /**
     * Runs a loop asynchronously in the background
     * @param block lambda to be executed on every iteration of loop until `stopWhen(condition)` written inside lambda becomes true
     * @return job that you can await by using `join` method, calling `join` will block execution until loop finishes
     * Note: loop uses default loopInterval of `50 millis`
     **/
    fun loopAsync(block: suspend StopWhen.() -> Unit): Job = coroutineScope.launch { loop(loopInterval, block) }

    /**
     * Runs a loop asynchronously in the background
     * @param minInterval every iteration of loop at least waits for provided minInterval before executing block of code
     * @param block lambda to be executed on every iteration of loop until `stopWhen(condition)` written inside lambda becomes true
     * @return job that you can await by using `join` method, calling `join` will block execution until loop finishes
     * Note: minInterval should be greater than `50 millis` otherwise default interval of 50 millis will be considered
     */
    fun loopAsync(minInterval: Duration, block: suspend StopWhen.() -> Unit): Job =
            coroutineScope.launch { loop(minInterval, block) }

    // ========== INTERNAL ===========
    private suspend fun loop0(minInterval: Duration, block: suspend StopWhen.() -> Unit) = coroutineScope {
        launch {
            suspend fun go() {
                delayedResult(maxOf(minInterval, loopInterval), block)
                go()
            }

            go()
        }
    }

    private suspend fun <T> delayedResult(minDelay: Duration, block: suspend StopWhen.() -> T): T = coroutineScope {
        val futureValue = async { block(StopWhen) }
        delay(minDelay.toLongMilliseconds())
        futureValue.await()
    }

    object StopWhen {
        // to be used within loop/loopAsync and breaks the loop if condition is true
        suspend fun stopWhen(condition: Boolean): Unit = coroutineScope {
            suspendCancellableCoroutine<Unit> {
                if (condition) it.cancel() else it.resumeWith(Result.success(Unit))
            }
        }
    }
}

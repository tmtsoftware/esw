package esw.ocs.dsl.highlevel

import kotlinx.coroutines.*
import kotlin.time.Duration
import kotlin.time.milliseconds

interface LoopDsl {
    companion object {
        private val loopInterval: Duration = 50.milliseconds
    }

    val coroutineScope: CoroutineScope

    /****** Sequential loops *******/
    suspend fun loop(block: suspend StopWhen.() -> Unit): Job = loop(loopInterval, block)

    suspend fun loop(minInterval: Duration, block: suspend StopWhen.() -> Unit): Job = loop0(minInterval, block)

    /****** Background loops *******/
    fun bgLoop(block: suspend StopWhen.() -> Unit): Deferred<Job> = coroutineScope.async { loop(loopInterval, block) }

    fun bgLoop(minInterval: Duration, block: suspend StopWhen.() -> Unit): Deferred<Job> =
        coroutineScope.async { loop(minInterval, block) }

    /****** Waiting for condition to be true *******/
    suspend fun waitFor(condition: suspend () -> Boolean) = loop { stopWhen(condition()) }

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
        suspend fun stopWhen(condition: Boolean): Unit = coroutineScope {
            suspendCancellableCoroutine<Unit> {
                if (condition) it.cancel() else it.resumeWith(Result.success(Unit))
            }
        }
    }
}

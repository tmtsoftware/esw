package esw.ocs.dsl.utils

import kotlin.time.Duration
import kotlin.time.milliseconds
import kotlinx.coroutines.*

private val loopInterval = 50.milliseconds

/****** Sequential loops *******/
suspend fun loop(block: suspend LoopDsl.() -> Unit): Job = loop(loopInterval, block)

suspend fun loop(minInterval: Duration, block: suspend LoopDsl.() -> Unit): Job = loop0(minInterval, block)

/****** Background loops *******/
fun CoroutineScope.bgLoop(block: suspend LoopDsl.() -> Unit): Deferred<Job> = async { loop(loopInterval, block) }

fun CoroutineScope.bgLoop(minInterval: Duration, block: suspend LoopDsl.() -> Unit): Deferred<Job> =
    async { loop(minInterval, block) }

object LoopDsl {
    suspend fun stopWhen(condition: Boolean): Unit = coroutineScope {
        suspendCancellableCoroutine<Unit> {
            if (condition) it.cancel() else it.resumeWith(Result.success(Unit))
        }
    }
}

// ========== INTERNAL ===========
private suspend fun loop0(minInterval: Duration, block: suspend LoopDsl.() -> Unit) = coroutineScope {
    launch {
        suspend fun go() {
            delayedResult(maxOf(minInterval, loopInterval), block)
            go()
        }

        go()
    }
}

private suspend fun <T> delayedResult(minDelay: Duration, block: suspend LoopDsl.() -> T): T = coroutineScope {
    val futureValue = async { block(LoopDsl) }
    delay(minDelay.toLongMilliseconds())
    futureValue.await()
}

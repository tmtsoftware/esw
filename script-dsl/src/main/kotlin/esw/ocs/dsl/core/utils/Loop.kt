package esw.ocs.dsl.core.utils

import kotlin.time.Duration
import kotlin.time.milliseconds
import kotlin.time.seconds
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

// =========== Sample Usage ===========
fun main() = runBlocking {
    var counter = 0
    var counter2 = 0

    bgLoop(1.seconds) {
        counter2 += 1
        println("[Bg Loop1] before stopIf, counter=$counter2")
        stopWhen(counter2 == 10)
        println("[Bg Loop1] after stopIf, counter=$counter2")
    }

    loop(1.seconds) {
        counter += 1
        println("[Loop1] before stopIf, counter=$counter")
        stopWhen(counter == 5)
        println("[Loop1] after stopIf, counter=$counter")
    }

    loop(1.seconds) {
        counter += 1
        println("[Loop2] before stopIf, counter=$counter")
        stopWhen(counter == 10)
        println("[Loop2] after stopIf, counter=$counter")
    }

    println("==============")
}

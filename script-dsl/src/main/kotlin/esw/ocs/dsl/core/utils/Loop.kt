package esw.ocs.dsl.core.utils

import kotlinx.coroutines.*
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.seconds

@ExperimentalTime
private val loopInterval = 50.milliseconds

@ExperimentalTime
suspend fun loop(block: suspend LoopDsl.() -> Unit): Job = loop(loopInterval, block)

@ExperimentalTime
suspend fun loop(minimumInterval: Duration, block: suspend LoopDsl.() -> Unit): Job = coroutineScope {
    launch {
        loopWithoutDelay { delayedResult(maxOf(minimumInterval, loopInterval), block) }
    }
}

@ExperimentalTime
fun CoroutineScope.bgLoop(block: suspend LoopDsl.() -> Unit): Deferred<Job> = async { loop(loopInterval, block) }

@ExperimentalTime
fun CoroutineScope.bgLoop(minimumInterval: Duration, block: suspend LoopDsl.() -> Unit): Deferred<Job> =
    async { loop(minimumInterval, block) }

private suspend fun loopWithoutDelay(block: suspend () -> Unit) {
    block()
    loopWithoutDelay(block)
}

@ExperimentalTime
private suspend fun <T> delayedResult(minDelay: Duration, block: suspend LoopDsl.() -> T): T = coroutineScope {
    val futureValue = async { block(LoopDsl) }
    delay(minDelay.toLongMilliseconds())
    futureValue.await()
}

object LoopDsl {
    suspend fun stopIf(condition: Boolean): Unit = coroutineScope {
        suspendCancellableCoroutine<Unit> {
            if (condition) it.cancel() else it.resumeWith(Result.success(Unit))
        }
    }
}

// =========== Sample Usage ===========
@ExperimentalTime
fun main() = runBlocking {
    var counter = 0
    var counter2 = 0

    bgLoop(1.seconds) {
        counter2 += 1
        println("[Bg Loop1] before stopIf, counter=$counter2")
        stopIf(counter2 == 10)
        println("[Bg Loop1] after stopIf, counter=$counter2")
    }

    loop(1.seconds) {
        counter += 1
        println("[Loop1] before stopIf, counter=$counter")
        stopIf(counter == 5)
        println("[Loop1] after stopIf, counter=$counter")
    }

    loop(1.seconds) {
        counter += 1
        println("[Loop2] before stopIf, counter=$counter")
        stopIf(counter == 10)
        println("[Loop2] after stopIf, counter=$counter")
    }

    println("==============")
}

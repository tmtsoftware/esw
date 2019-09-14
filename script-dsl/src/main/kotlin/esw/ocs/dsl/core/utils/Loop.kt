package esw.ocs.dsl.core.utils

import kotlinx.coroutines.*
import kotlin.time.*

@UseExperimental(ExperimentalTime::class)
class Loop {
    private val loopInterval = 50.milliseconds

    inner class StopIf {
        suspend fun stopIf(condition: Boolean): Unit = coroutineScope {
            suspendCancellableCoroutine<Unit> {
                if (condition) it.cancel() else it.resumeWith(Result.success(Unit))
            }
        }
    }

    suspend fun loop(block: suspend () -> Unit): Job = loop(loopInterval, block)

    suspend fun loop(minimumInterval: Duration, block: suspend () -> Unit): Job = coroutineScope {
        launch {
            loopWithoutDelay { delayedResult(maxOf(minimumInterval, loopInterval), block) }
        }
    }

    private suspend fun loopWithoutDelay(block: suspend () -> Unit) {
        block()
        loopWithoutDelay(block)
    }

    private suspend fun <T> delayedResult(minDelay: Duration, block: suspend () -> T): T = coroutineScope {
        val futureValue = async { block() }
        delay(minDelay.toLongMilliseconds())
        futureValue.await()
    }
}

@ExperimentalTime
suspend fun loop(block: suspend Loop.StopIf.() -> Unit) {
    Loop().run { loop { block(StopIf()) } }
}

@ExperimentalTime
suspend fun loop(minimumInterval: Duration, block: suspend Loop.StopIf.() -> Unit) {
    Loop().run { loop(minimumInterval) { block(StopIf()) } }
}

// =========== Sample Usage ===========
@ExperimentalTime
fun main() = runBlocking {
    var counter = 0

    loop(1.seconds) {
        counter += 1
        println("Before stopIf, counter=$counter")
        stopIf(counter == 5)
        println("After stopIf, counter=$counter")
    }
}

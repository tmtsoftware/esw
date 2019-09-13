package esw.ocs.dsl.core.utils

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import kotlin.time.*

@UseExperimental(ExperimentalTime::class)
class Loop {
    private val loopInterval = 50.milliseconds

    // lightweight throwable without the stack trace
    private object LoopStopException : Throwable("Stop loop", null, false, false)

    suspend fun loop(block: suspend () -> Unit): Unit = loop(loopInterval, block)

    suspend fun loop(minimumInterval: Duration, block: suspend () -> Unit): Unit =
        loopWithoutDelay { delayedResult(maxOf(minimumInterval, loopInterval), block) }

    inner class StopIf {
        fun stopIf(condition: Boolean) {
            if (condition) throw LoopStopException
        }
    }

    private suspend fun loopWithoutDelay(block: suspend () -> Unit) {
        try {
            block()
            loopWithoutDelay(block)
        } catch (e: LoopStopException) {
        }
    }

    private suspend fun <T> delayedResult(minDelay: Duration, f: suspend () -> T): T = coroutineScope {
        val futureValue = async { f() }
        delay(minDelay.toJavaDuration())
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
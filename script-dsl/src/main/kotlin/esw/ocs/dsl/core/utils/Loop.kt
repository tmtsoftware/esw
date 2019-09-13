package esw.ocs.dsl.core.utils

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.*

@UseExperimental(ExperimentalTime::class)
class Loop {
    private val stop = AtomicBoolean(false)
    private val loopInterval = 50.milliseconds

    suspend fun loop(block: suspend () -> Unit): Unit = loop(loopInterval, block)

    suspend fun loop(minimumInterval: Duration, block: suspend () -> Unit): Unit =
        loopWithoutDelay { delayedResult(maxOf(minimumInterval, loopInterval), block) }

    inner class StopIf {
        //todo: loop should terminate if this condition matches without executing rest of the code
        fun stopIf(condition: Boolean) {
            // once stop condition matches, then do not override it with following false conditions
            if (condition) stop.set(condition)
        }
    }

    private suspend fun loopWithoutDelay(block: suspend () -> Unit) {
        block()
        if (stop.get()) return else loopWithoutDelay(block)
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
        println(counter)
        stopIf(counter == 31)

        counter *= 2
        println(counter)
        stopIf(counter == 20)
    }

}
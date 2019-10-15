package esw.ocs.dsl.utils

import kotlin.time.seconds
import kotlinx.coroutines.runBlocking

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

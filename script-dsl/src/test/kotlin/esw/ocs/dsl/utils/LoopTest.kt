package esw.ocs.dsl.utils

import esw.ocs.dsl.core.utils.loop
import io.kotlintest.matchers.numerics.shouldBeInRange
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis
import kotlin.time.milliseconds

class LoopTest : StringSpec({
    "loop must run till condition becomes true when interval is default | ESW-89" {
        val counter = AtomicInteger(0)

        val loopTime = measureTimeMillis {
            loop {
                counter.getAndUpdate { it + 1 }
                stopWhen(counter.get() == 5)
            }
        }

        counter.get() shouldBe 5
        // default interval is 50ms, loop should run 5 times which means it should take around 50*5=250ms
        loopTime shouldBeInRange 250L..300L
    }

    "loop must run till condition becomes true when interval is custom | ESW-89" {
        val counter = AtomicInteger(0)

        val loopTime = measureTimeMillis {
            loop(300.milliseconds) {
                counter.getAndUpdate { it + 1 }
                stopWhen(counter.get() == 3)
            }
        }

        counter.get() shouldBe 3
        // custom loop interval is 300ms, loop should run 3 times which means it should take around 300*3=900ms
        loopTime shouldBeInRange 900L..1000L
    }
})

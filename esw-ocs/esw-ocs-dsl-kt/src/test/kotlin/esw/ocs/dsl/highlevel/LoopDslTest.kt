package esw.ocs.dsl.highlevel

import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.longs.shouldBeInRange
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.milliseconds

@Suppress("DANGEROUS_CHARACTERS")
class LoopDslTest : LoopDsl {
    override val coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)

    @Test
    fun `loop_should_run_till_condition_becomes_true_when_interval_is_default_|_ESW-89`() = runBlocking {
        val counter = AtomicInteger(0)

        val loopTime = measureTimeMillis {
            loop {
                counter.getAndUpdate { it + 1 }
                stopWhen(counter.get() == 5)
            }
        }

        counter.get() shouldBe 5
        // default interval is 50ms, loop should run 5 times which means it should take around 50*5=250ms
        loopTime shouldBeInRange 250L..350L
    }

    @Test
    fun `loop_should_run_till_condition_becomes_true_when_interval_is_custom_|_ESW-89`() = runBlocking {
        val counter = AtomicInteger(0)

        val loopTime = measureTimeMillis {
            loop(300.milliseconds) {
                counter.getAndUpdate { it + 1 }
                stopWhen(counter.get() == 3)
            }
        }

        counter.get() shouldBe 3
        // custom loop interval is 300ms, loop should run 3 times that means it should take around 300*3=900ms
        loopTime shouldBeInRange 900L..1000L
    }

    @Test
    fun `loop_should_allow_stopWhen_conditions_to_be_specified_any_number_of_times_and_anywhere_in_the_loop_body_|_ESW-89`() =
        runBlocking {
            val counter1 = AtomicInteger(0)
            val counter2 = AtomicInteger(0)

            loop {
                counter1.getAndUpdate { it + 1 }
                stopWhen(counter1.get() == 5)

                counter2.getAndUpdate { it + 2 }
                stopWhen(counter2.get() == 10)
            }

            // on 5th iteration, counter1 becomes 5 and first stopWhen matches
            // loop gets terminated there and does not execute rest of the body
            counter1.get() shouldBe 5
            counter2.get() shouldBe 8
        }

    @Test
    fun `loopAsync_should_run_in_the_background_till_condition_becomes_true_when_interval_is_default_|_ESW-89`() =
        runBlocking {
            val counter = AtomicInteger(0)

            val loopTime = measureTimeMillis {
                val loopResult = loopAsync {
                    counter.getAndUpdate { it + 1 }
                    stopWhen(counter.get() == 5)
                }

                // here counter is less than 5 proves that loop is still running in the background
                counter.get() shouldBeLessThan 5
                loopResult.join()
            }

            counter.get() shouldBe 5
            // default interval is 50ms, loop should run 5 times which means it should take around 50*5=250ms
            loopTime shouldBeInRange 250L..350L
        }

    @Test
    fun `loopAsync_should_run_in_the_background_till_condition_becomes_true_when_interval_is_custom_|_ESW-89`() =
        runBlocking {
            val counter = AtomicInteger(0)

            val loopTime = measureTimeMillis {
                val loopResult = loopAsync(300.milliseconds) {
                    counter.getAndUpdate { it + 1 }
                    stopWhen(counter.get() == 3)
                }

                // here counter is less than 3 proves that loop is still running in the background
                counter.get() shouldBeLessThan 3
                loopResult.join()
            }

            counter.get() shouldBe 3
            // custom loop interval is 300ms, loop should run 3 times that means it should take around 300*3=900ms
            loopTime shouldBeInRange 900L..1000L
        }

    @Test
    fun `waitFor_should_wait_for_condition_to_become_true_before_executing_rest_of_the_loop_body_|_ESW-89`() =
        runBlocking {
            val numExposures = 5
            var exposureCount = 0
            var exposureReady = false

            // simulate background task that is setting exposureReady flag to true after 200 ms
            launch {
                delay(200)
                exposureReady = true
            }

            exposureReady shouldBe false
            loop {
                waitFor { exposureReady }
                exposureReady shouldBe true

                exposureCount += 1
                stopWhen(exposureCount == numExposures)
            }

            exposureCount shouldBe 5
        }
}

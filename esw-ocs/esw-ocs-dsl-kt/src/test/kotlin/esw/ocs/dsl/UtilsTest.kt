package esw.ocs.dsl

import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis

@Suppress("DANGEROUS_CHARACTERS")
class UtilsTest {

    @Test
    fun `par_should_execute_provided_tasks_concurrently_and_return_result_once_all_tasks_finished_|_ESW-87`() {
        runBlocking {
            suspend fun submitCommand(): Int {
                delay(100)
                return 42
            }

            val timeTaken = measureTimeMillis {
                val aggregatedResult = par(
                        { submitCommand() },
                        { submitCommand() },
                        { submitCommand() },
                        { submitCommand() },
                        { submitCommand() }
                )

                aggregatedResult shouldBe listOf(42, 42, 42, 42, 42)
            }

            // the individual operation takes 100ms, and we are running such 5 operations
            // because these are running concurrently, total time taken is less than 250ms and not 500ms
            timeTaken shouldBeLessThan 250
        }
    }

    @Test
    fun `should_execute_provided_tasks_sequentially_by_default_and_return_result_once_all_tasks_finished_|_ESW-88`() {
        runBlocking {
            suspend fun submitCommand(): Int {
                delay(100)
                return 42
            }

            val timeTaken = measureTimeMillis {
                val r1 = submitCommand()
                val r2 = submitCommand()
                val r3 = submitCommand()
                val r4 = submitCommand()
                val r5 = submitCommand()

                listOf(r1, r2, r3, r4, r5) shouldBe listOf(42, 42, 42, 42, 42)
            }

            // the individual operation takes 100ms, and we are running such 5 operations
            // because these are running sequentially, total time taken should be equal to or greater than 500ms
            timeTaken shouldBeGreaterThanOrEqual 500
        }
    }
}

package esw.ocs.dsl.utils

import esw.ocs.dsl.Dsl
import io.kotlintest.matchers.numerics.shouldBeLessThan
import io.kotlintest.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis

class DslTest : Dsl {

    @Test
    fun `par should execute provided tasks concurrently and return result once all tasks finished | ESW-87`() = runBlocking {
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

        // the individual operation takes 100ms and we are running such 5 operations
        // because these are running concurrently, total time taken is less than 250ms and not 500ms
        timeTaken shouldBeLessThan 250
    }
}

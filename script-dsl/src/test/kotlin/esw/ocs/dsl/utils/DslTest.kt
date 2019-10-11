package esw.ocs.dsl.utils

import esw.ocs.dsl.Dsl
import io.kotlintest.matchers.numerics.shouldBeLessThan
import io.kotlintest.shouldBe
import io.kotlintest.specs.WordSpec
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.delay

class DslTest : Dsl, WordSpec() {

    init {
        "par" should {
            "execute provided tasks concurrently and return result once all tasks finished | ESW-87" {
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
    }
}

package esw.ocs.framework.dsl

import esw.ocs.framework.BaseTestSuite
import esw.ocs.framework.dsl.internal.FunctionHandlers

import scala.collection.mutable.ArrayBuffer
import scala.util.Success

class FunctionHandlersTest extends BaseTestSuite {
  "execute" when {
    "handlers are empty" should {
      "return empty buffer" in {
        val functionHandlers = new FunctionHandlers[Int, String]
        functionHandlers.execute(1) shouldBe empty
      }
    }
    "handlers are added" should {
      "return output in order" in {
        val functionHandlers = new FunctionHandlers[Int, Int]

        functionHandlers.add(number ⇒ number + 10)
        functionHandlers.add(number ⇒ number - 10)
        functionHandlers.add(number ⇒ number * 2)

        functionHandlers.execute(100) shouldBe ArrayBuffer(Success(110), Success(90), Success(200))
      }
    }
    "a handler throws exception" should {
      "return output in order with Failures" in {
        val functionHandlers = new FunctionHandlers[Int, Int]

        functionHandlers.add(number ⇒ number + 10)
        functionHandlers.add(number ⇒ number / 0)
        functionHandlers.add(number ⇒ number * 2)

        val results = functionHandlers.execute(100)
        results.head shouldBe Success(110)
        results.last shouldBe Success(200)

        an[ArithmeticException] should be thrownBy results(1).get
      }
    }
  }
}

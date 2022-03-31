/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.dsl.script.utils

import esw.testcommons.BaseTestSuite

import scala.collection.mutable.ArrayBuffer

class FunctionHandlersTest extends BaseTestSuite {
  "execute" must {
    "return empty buffer when handlers are empty | ESW-90" in {
      val functionHandlers = new FunctionHandlers[Int, String]
      functionHandlers.execute(1) shouldBe empty
    }

    "return output in order when handlers are added | ESW-90" in {
      val functionHandlers = new FunctionHandlers[Int, Int]

      functionHandlers.add(number => number + 10)
      functionHandlers.add(number => number - 10)
      functionHandlers.add(number => number * 2)

      functionHandlers.execute(100) shouldBe ArrayBuffer(110, 90, 200)
    }

    "throw an exception when one of the handler fails | ESW-90" in {
      val functionHandlers = new FunctionHandlers[Int, Int]

      functionHandlers.add(number => number + 10)
      functionHandlers.add(number => number / 0)
      functionHandlers.add(number => number * 2)

      an[ArithmeticException] should be thrownBy functionHandlers.execute(100)
    }
  }
}

/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.gateway.impl

import csw.prefix.models.Prefix
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class LoggerCacheTest extends AnyWordSpecLike with Matchers {
  "get" must {
    "return the same instance of logger for same names | ESW-200, CSW-63, CSW-78" in {
      val loggerCache = new LoggerCache
      val logger1     = loggerCache.get(Prefix("esw.a"))
      val logger2     = loggerCache.get(Prefix("esw.a"))
      logger1 should ===(logger2)
    }

    "return a different instance of logger for different names | ESW-200, CSW-63, CSW-78" in {
      val loggerCache = new LoggerCache
      val logger1     = loggerCache.get(Prefix("esw.a"))
      val logger2     = loggerCache.get(Prefix("esw.b"))
      logger1 should !==(logger2)
    }
  }
}

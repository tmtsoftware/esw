package esw.gateway.impl

import org.scalatest.{Matchers, WordSpecLike}

class LoggerCacheTest extends WordSpecLike with Matchers {
  "get" must {
    "return the same instance of logger for same names | ESW-200" in {
      val loggerCache = new LoggerCache
      val logger1     = loggerCache.get("a")
      val logger2     = loggerCache.get("a")
      logger1 should ===(logger2)
    }

    "return a different instance of logger for different names | ESW-200" in {
      val loggerCache = new LoggerCache
      val logger1     = loggerCache.get("a")
      val logger2     = loggerCache.get("b")
      logger1 should !==(logger2)
    }
  }
}

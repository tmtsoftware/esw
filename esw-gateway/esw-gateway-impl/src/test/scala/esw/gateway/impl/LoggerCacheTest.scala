package esw.gateway.impl

import csw.prefix.models.Prefix
import org.scalatest.{Matchers, WordSpecLike}

class LoggerCacheTest extends WordSpecLike with Matchers {
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

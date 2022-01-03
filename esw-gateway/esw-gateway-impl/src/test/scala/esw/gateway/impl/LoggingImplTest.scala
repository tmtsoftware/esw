package esw.gateway.impl

import akka.Done
import csw.logging.api.scaladsl.Logger
import csw.logging.models.Level.*
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.IRIS
import esw.gateway.api.LoggingApi
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar

class LoggingImplTest extends AnyWordSpecLike with ScalaFutures with Matchers with MockitoSugar {
  private val cache    = mock[LoggerCache]
  private val prefix   = Prefix(IRIS, "filter.wheel")
  private val logger   = mock[Logger]
  private val logMsg   = "Hello World"
  private val metadata = Map("prefix" -> prefix)

  "LoggingImpl" must {
    "delegate to underlying LoggerApi" in {
      when(cache.get(prefix)).thenReturn(logger)

      val loggerImpl: LoggingApi = new LoggingImpl(cache)

      loggerImpl.log(prefix, TRACE, logMsg, metadata).futureValue should ===(Done)
      verify(logger).trace(logMsg, metadata)

      loggerImpl.log(prefix, DEBUG, logMsg, metadata).futureValue should ===(Done)
      verify(logger).debug(logMsg, metadata)

      loggerImpl.log(prefix, INFO, logMsg, metadata).futureValue should ===(Done)
      verify(logger).info(logMsg, metadata)

      loggerImpl.log(prefix, WARN, logMsg, metadata).futureValue should ===(Done)
      verify(logger).warn(logMsg, metadata)

      loggerImpl.log(prefix, ERROR, logMsg, metadata).futureValue should ===(Done)
      verify(logger).error(logMsg, metadata)

      loggerImpl.log(prefix, FATAL, logMsg, metadata).futureValue should ===(Done)
      verify(logger).fatal(logMsg, metadata)
    }
  }
}

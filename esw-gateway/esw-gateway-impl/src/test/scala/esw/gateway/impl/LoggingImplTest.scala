package esw.gateway.impl

import akka.Done
import csw.logging.api.NoLogException
import csw.logging.api.scaladsl.Logger
import csw.logging.macros.SourceFactory
import csw.logging.models.Level._
import csw.logging.models.noId
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.IRIS
import org.mockito.ArgumentMatchers.{any, eq => argEq}
import org.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class LoggingImplTest extends AnyWordSpecLike with ScalaFutures with Matchers with MockitoSugar {
  private val cache    = mock[LoggerCache]
  private val prefix   = Prefix(IRIS, "filter.wheel")
  private val logger   = mock[Logger]
  private val logMsg   = "Hello World"
  private val metadata = Map("prefix" -> prefix)

  "LoggingImpl" must {
    "delegate to underlying LoggerApi" in {
      when(cache.get(prefix)).thenReturn(logger)

      doNothing.when(logger).trace(argEq(logMsg), argEq(metadata), argEq(NoLogException), argEq(noId))(argEq(any[SourceFactory]))
      doNothing.when(logger).debug(argEq(logMsg), argEq(metadata), argEq(NoLogException), argEq(noId))(argEq(any[SourceFactory]))
      doNothing.when(logger).info(argEq(logMsg), argEq(metadata), argEq(NoLogException), argEq(noId))(argEq(any[SourceFactory]))
      doNothing.when(logger).warn(argEq(logMsg), argEq(metadata), argEq(NoLogException), argEq(noId))(argEq(any[SourceFactory]))
      doNothing.when(logger).error(argEq(logMsg), argEq(metadata), argEq(NoLogException), argEq(noId))(argEq(any[SourceFactory]))
      doNothing.when(logger).fatal(argEq(logMsg), argEq(metadata), argEq(NoLogException), argEq(noId))(argEq(any[SourceFactory]))

      val loggerImpl = new LoggingImpl(cache)

      loggerImpl.log(prefix, TRACE, logMsg, metadata).futureValue should ===(Done)
      verify(logger).trace(argEq(logMsg), argEq(metadata), argEq(NoLogException), argEq(noId))(argEq(any[SourceFactory]))

      loggerImpl.log(prefix, DEBUG, logMsg, metadata).futureValue should ===(Done)
      verify(logger).debug(argEq(logMsg), argEq(metadata), argEq(NoLogException), argEq(noId))(argEq(any[SourceFactory]))

      loggerImpl.log(prefix, INFO, logMsg, metadata).futureValue should ===(Done)
      verify(logger).info(argEq(logMsg), argEq(metadata), argEq(NoLogException), argEq(noId))(argEq(any[SourceFactory]))

      loggerImpl.log(prefix, WARN, logMsg, metadata).futureValue should ===(Done)
      verify(logger).warn(argEq(logMsg), argEq(metadata), argEq(NoLogException), argEq(noId))(argEq(any[SourceFactory]))

      loggerImpl.log(prefix, ERROR, logMsg, metadata).futureValue should ===(Done)
      verify(logger).error(argEq(logMsg), argEq(metadata), argEq(NoLogException), argEq(noId))(argEq(any[SourceFactory]))

      loggerImpl.log(prefix, FATAL, logMsg, metadata).futureValue should ===(Done)
      verify(logger).fatal(argEq(logMsg), argEq(metadata), argEq(NoLogException), argEq(noId))(argEq(any[SourceFactory]))
    }
  }
}

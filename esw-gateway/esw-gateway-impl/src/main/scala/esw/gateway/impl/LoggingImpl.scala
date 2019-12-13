package esw.gateway.impl

import akka.Done
import csw.logging.models.Level
import csw.prefix.models.Prefix
import esw.gateway.api.LoggingApi

import scala.concurrent.Future

class LoggingImpl(loggerCache: LoggerCache) extends LoggingApi {
  override def log(prefix: Prefix, level: Level, message: String, metadata: Map[String, Any]): Future[Done] = {
    val logger = loggerCache.get(prefix)
    level match {
      case Level.TRACE => logger.trace(message, metadata)
      case Level.DEBUG => logger.debug(message, metadata)
      case Level.INFO  => logger.info(message, metadata)
      case Level.WARN  => logger.warn(message, metadata)
      case Level.ERROR => logger.error(message, metadata)
      case Level.FATAL => logger.fatal(message, metadata)
    }
    Future.successful(Done)
  }
}

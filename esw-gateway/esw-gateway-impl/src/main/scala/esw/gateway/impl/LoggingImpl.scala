package esw.gateway.impl

import akka.Done
import csw.logging.models.Level
import esw.gateway.api.LoggingApi

import scala.concurrent.Future

class LoggingImpl(loggerCache: LoggerCache) extends LoggingApi {
  override def log(appName: String, level: Level, message: String, metadata: Map[String, Any]): Future[Done] = {
    val logger = loggerCache.get(appName)
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

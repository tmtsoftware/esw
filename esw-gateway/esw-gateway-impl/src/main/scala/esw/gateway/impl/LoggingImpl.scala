package esw.gateway.impl

import akka.Done
import csw.logging.models.Level
import esw.gateway.api.LoggingApi
import io.bullet.borer.Dom.{MapElem, _}

import scala.concurrent.Future

class LoggingImpl(loggerCache: LoggerCache) extends LoggingApi {

  private def getValue(element: Element): Any = {
    element match {
      case BooleanElem(value)      => value
      case StringElem(value)       => value
      case IntElem(value)          => value
      case LongElem(value)         => value
      case NumberStringElem(value) => value
      case DoubleElem(value)       => value
      case FloatElem(value)        => value
      case Float16Elem(value)      => value
      case elem: ArrayElem         => Array(elem.elements.map(getValue))
      case elem: MapElem           => getMap(elem)
      case _                       => null
    }
  }

  private def getMap(element: MapElem): Map[String, Any] =
    element
      .asInstanceOf[MapElem]
      .toMap
      .map[String, Any] {
        case (k, v) => (k.asInstanceOf[StringElem].value, getValue(v))
      }
      .filter({
        case (_, v) => v != null
      })

  override def log(appName: String, level: Level, message: String, metadata: MapElem): Future[Done] = {
    val logger = loggerCache.get(appName)
    val map    = getMap(metadata)
    level match {
      case Level.TRACE => logger.trace(message, map)
      case Level.DEBUG => logger.debug(message, map)
      case Level.INFO  => logger.info(message, map)
      case Level.WARN  => logger.warn(message, map)
      case Level.ERROR => logger.error(message, map)
      case Level.FATAL => logger.fatal(message, map)
    }
    Future.successful(Done)
  }
}

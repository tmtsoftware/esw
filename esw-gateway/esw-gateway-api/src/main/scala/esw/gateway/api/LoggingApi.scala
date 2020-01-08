package esw.gateway.api

import akka.Done
import csw.logging.models.Level
import csw.prefix.models.Prefix

import scala.concurrent.Future

trait LoggingApi {
  def log(prefix: Prefix, level: Level, message: String, metadata: Map[String, Any] = Map.empty): Future[Done]
}

package esw.gateway.api

import akka.Done
import csw.logging.models.Level

import scala.concurrent.Future

trait LoggingApi {
  def log(appName: String, level: Level, message: String, metadata: Map[String, Any]): Future[Done]
}

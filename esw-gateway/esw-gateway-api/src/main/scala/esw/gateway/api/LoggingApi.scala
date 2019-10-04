package esw.gateway.api

import akka.Done
import csw.logging.models.Level
import io.bullet.borer.Dom.MapElem

import scala.concurrent.Future

trait LoggingApi {
  def log(appName: String, level: Level, message: String, metadata: MapElem): Future[Done]
}

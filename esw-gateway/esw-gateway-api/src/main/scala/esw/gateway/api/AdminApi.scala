package esw.gateway.api

import akka.Done
import csw.location.api.models.ComponentId
import csw.logging.models.{Level, LogMetadata}

import scala.concurrent.Future

trait AdminApi {
  def getLogMetadata(componentId: ComponentId): Future[LogMetadata]
  def setLogLevel(componentId: ComponentId, level: Level): Future[Done]
}

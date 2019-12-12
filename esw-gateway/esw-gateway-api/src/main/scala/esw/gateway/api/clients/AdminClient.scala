package esw.gateway.api.clients

import csw.admin.api.AdminService
import csw.location.models.ComponentId
import csw.logging.models.codecs.LoggingCodecs
import csw.logging.models.{Level, LogMetadata}
import esw.gateway.api.protocol.PostRequest
import esw.gateway.api.protocol.PostRequest.{GetLogMetadata, SetLogLevel}
import msocket.api.Transport

import scala.concurrent.Future

class AdminClient(postClient: Transport[PostRequest]) extends AdminService with LoggingCodecs {
  def getLogMetadata(componentId: ComponentId): Future[LogMetadata] =
    postClient.requestResponse[LogMetadata](GetLogMetadata(componentId))
  def setLogLevel(componentId: ComponentId, level: Level): Future[Unit] =
    postClient.requestResponse[Unit](SetLogLevel(componentId, level))
}

package esw.gateway.api.clients

import akka.Done
import csw.logging.models.Level
import esw.gateway.api.LoggingApi
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.PostRequest
import esw.gateway.api.protocol.PostRequest.Log
import io.bullet.borer.Dom.MapElem
import msocket.api.Transport

import scala.concurrent.Future

class LoggingClient(postClient: Transport[PostRequest]) extends LoggingApi with GatewayCodecs {
  def log(appName: String, level: Level, message: String, metadata: MapElem): Future[Done] = {
    postClient.requestResponse[Done](Log(appName, level, message, metadata))
  }
}

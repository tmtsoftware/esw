package esw.gateway.server.metrics

import akka.http.scaladsl.server.Route
import esw.gateway.api.codecs.GatewayCodecs._
import esw.gateway.api.protocol.PostRequest
import esw.gateway.api.protocol.PostRequest.{ComponentCommand, GetEvent, PublishEvent}
import msocket.impl.post.HttpPostHandler

class PostHandlerMetrics(postHandler: HttpPostHandler[PostRequest]) extends HttpPostHandler[PostRequest] {
  import CommandMetrics._
  import EventMetrics._

  override def handle(request: PostRequest): Route = {
    request match {
      case ComponentCommand(_, command) => incCommandCounter(command)
      case _: PublishEvent              => incEventCounter(publishEventLabel)
      case _: GetEvent                  => incEventCounter(getEventLabel)
      case _                            =>
    }
    postHandler.handle(request)
  }
}

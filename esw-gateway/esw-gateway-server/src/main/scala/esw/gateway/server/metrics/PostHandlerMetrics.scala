package esw.gateway.server.metrics

import akka.http.scaladsl.server.Route
import esw.gateway.api.protocol.PostRequest
import msocket.impl.post.HttpPostHandler
import esw.gateway.api.codecs.GatewayCodecs._

class PostHandlerMetrics(postHandler: HttpPostHandler[PostRequest]) extends HttpPostHandler[PostRequest] {
  import CommandMetrics._
  import EventMetrics._

  override def handle(request: PostRequest): Route = {
    request match {
      case PostRequest.ComponentCommand(_, command) => incCommandCounter(command)
      case _: PostRequest.PublishEvent              => incEventCounter(publishEventLabel)
      case _: PostRequest.GetEvent                  => incEventCounter(getEventLabel)
      case _                                        =>
    }
    postHandler.handle(request)
  }
}

package esw.gateway.server.metrics

import akka.http.scaladsl.server.Route
import esw.gateway.api.codecs.GatewayCodecs._
import esw.gateway.api.protocol.PostRequest
import esw.gateway.api.protocol.PostRequest.{ComponentCommand, SequencerCommand}
import msocket.impl.post.HttpPostHandler

class PostHandlerMetrics(postHandler: HttpPostHandler[PostRequest]) extends HttpPostHandler[PostRequest] {
  import Metrics._

  override def handle(request: PostRequest): Route = {
    val label = request match {
      case ComponentCommand(_, command) => createLabel(request, command)
      case SequencerCommand(_, command) => createLabel(request, command)
      case _                            => createLabel(request)
    }

    httpCounter.labels(label).inc()
    postHandler.handle(request)
  }
}

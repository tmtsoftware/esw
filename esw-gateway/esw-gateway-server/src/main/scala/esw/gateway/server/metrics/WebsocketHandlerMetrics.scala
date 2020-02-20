package esw.gateway.server.metrics

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.Source
import esw.gateway.api.codecs.GatewayCodecs._
import esw.gateway.api.protocol.WebsocketRequest
import esw.gateway.api.protocol.WebsocketRequest.{ComponentCommand, SequencerCommand}
import msocket.api.ContentType
import msocket.impl.ws.WebsocketHandler

import scala.concurrent.ExecutionContext

class WebsocketHandlerMetrics(wsHandler: WebsocketHandler[WebsocketRequest], contentType: ContentType)(
    implicit ec: ExecutionContext
) extends WebsocketHandler[WebsocketRequest](contentType) {
  import Metrics._

  override def handle(request: WebsocketRequest): Source[Message, NotUsed] = {
    val label = request match {
      case ComponentCommand(_, command) => createLabel(request, command)
      case SequencerCommand(_, command) => createLabel(request, command)
      case _                            => createLabel(request)
    }

    websocketGauge.labels(label).inc()
    wsHandler.handle(request).watchTermination() {
      case (mat, completion) =>
        completion.onComplete(_ => websocketGauge.labels(label).dec())
        mat
    }
  }
}

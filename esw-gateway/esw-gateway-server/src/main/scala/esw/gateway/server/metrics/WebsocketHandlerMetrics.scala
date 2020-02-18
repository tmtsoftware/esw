package esw.gateway.server.metrics

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.Source
import esw.gateway.api.codecs.GatewayCodecs._
import esw.gateway.api.protocol.WebsocketRequest
import msocket.api.ContentType
import msocket.impl.ws.WebsocketHandler

import scala.concurrent.ExecutionContext

class WebsocketHandlerMetrics(wsHandler: WebsocketHandler[WebsocketRequest], contentType: ContentType)(
    implicit ec: ExecutionContext
) extends WebsocketHandler[WebsocketRequest](contentType) {

  import CommandMetrics._
  import EventMetrics._

  override def handle(request: WebsocketRequest): Source[Message, NotUsed] = {
    lazy val response = wsHandler.handle(request)

    def onTermination(task: => Unit) = response.watchTermination() {
      case (mat, completion) =>
        completion.onComplete(_ => task)
        mat
    }

    request match {
      case WebsocketRequest.ComponentCommand(_, command) =>
        incCommandGauge(command)
        onTermination(decCommandGauge(command))

      case _: WebsocketRequest.SequencerCommand => response

      case _: WebsocketRequest.Subscribe =>
        incSubscriberGauge()
        onTermination(decSubscriberGauge())

      case WebsocketRequest.SubscribeWithPattern(subsystem, _, pattern) =>
        incPatternSubscriberGauge(subsystem, pattern)
        onTermination(decPatternSubscriberGauge(subsystem, pattern))
    }
  }
}

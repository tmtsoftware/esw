package esw.gateway.server.handlers

import akka.http.scaladsl.server.Route
import esw.gateway.api.protocol.PostRequest
import esw.gateway.api.protocol.PostRequest.{ComponentCommand, SequencerCommand}
import kamon.instrumentation.akka.http.TracingDirectives.operationName
import msocket.api.MessageHandler

object PostHandlerWithRichMetrics {
  implicit class PostHandlerWithRichMetrics(private val postHandlerImpl: PostHandlerImpl) extends AnyVal {
    def withMetrics(): MessageHandler[PostRequest, Route] = (request: PostRequest) => {
      def completeWithName[T](msg: T) = operationName(msg.getClass.getSimpleName) {
        postHandlerImpl.handle(request)
      }

      request match {
        case ComponentCommand(_, command) => completeWithName(command)
        case SequencerCommand(_, command) => completeWithName(command)
        case msg                          => completeWithName(msg)
      }
    }
  }
}

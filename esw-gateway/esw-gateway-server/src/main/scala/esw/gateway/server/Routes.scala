package esw.gateway.server

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route, StandardRoute}
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import csw.logging.api.scaladsl.Logger
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.{PostRequest, WebsocketRequest}
import msocket.impl.post.ServerHttpCodecs
import msocket.impl.ws.WsServerFlow
import msocket.api.MessageHandler
import msocket.impl.Encoding

import scala.util.control.NonFatal

class Routes(
    postHandler: MessageHandler[PostRequest, StandardRoute],
    websocketHandler: Encoding[_] => MessageHandler[WebsocketRequest, Source[Message, NotUsed]],
    log: Logger
)(implicit mat: Materializer)
    extends GatewayCodecs
    with ServerHttpCodecs {

  //fixme: Is this necessary? whats the default behavior of akka http on future failure?
  // Is there a better way to log?
  val commonExceptionHandlers: ExceptionHandler = ExceptionHandler {
    case NonFatal(ex) =>
      log.error(ex.getMessage, ex = ex)
      complete(HttpResponse(StatusCodes.InternalServerError))
  }

  //fixme: route logger
  val route: Route = handleExceptions(commonExceptionHandlers) {
    handleRejections(RejectionHandler.default) {
      get {
        path("websocket-endpoint") {
          handleWebSocketMessages {
            new WsServerFlow(websocketHandler).flow
          }
        }
      } ~
      post {
        path("post-endpoint") {
          entity(as[PostRequest])(postHandler.handle)
        }
      }
    }
  }
}

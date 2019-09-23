package esw.gateway.server

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route, StandardRoute}
import akka.stream.scaladsl.Source
import csw.logging.api.scaladsl.Logger
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.{PostRequest, WebsocketRequest}
import mscoket.impl.HttpCodecs
import mscoket.impl.ws.WsServerFlow
import msocket.api.MessageHandler

import scala.util.control.NonFatal

class Routes(
    postHandler: MessageHandler[PostRequest, StandardRoute],
    websocketHandler: MessageHandler[WebsocketRequest, Source[Message, NotUsed]],
    log: Logger
) extends GatewayCodecs
    with HttpCodecs {

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
        path("websocket") {
          handleWebSocketMessages {
            new WsServerFlow(websocketHandler).flow
          }
        }
      } ~
      post {
        path("post") {
          entity(as[PostRequest])(postHandler.handle)
        }
      }
    }
  }
}

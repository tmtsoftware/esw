package esw.gateway.server

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route, StandardRoute}
import akka.stream.scaladsl.Source
import csw.logging.api.scaladsl.Logger
import esw.gateway.api.codecs.RestlessCodecs
import esw.gateway.api.messages.{PostRequest, WebsocketRequest}
import mscoket.impl.HttpCodecs
import mscoket.impl.ws.WsServerFlow
import msocket.api.RequestHandler

import scala.util.control.NonFatal

class Routes(
    postHandler: RequestHandler[PostRequest, StandardRoute],
    websocketHandler: RequestHandler[WebsocketRequest, Source[Message, NotUsed]],
    log: Logger
) extends RestlessCodecs
    with HttpCodecs {

  val commonExceptionHandlers: ExceptionHandler = ExceptionHandler {
    case NonFatal(ex) =>
      log.error(ex.getMessage, ex = ex)
      complete(HttpResponse(StatusCodes.InternalServerError))
  }

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

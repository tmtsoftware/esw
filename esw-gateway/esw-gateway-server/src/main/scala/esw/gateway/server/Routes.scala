package esw.gateway.server

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, StandardRoute}
import akka.stream.scaladsl.Source
import esw.gateway.api.codecs.RestlessCodecs
import esw.gateway.api.messages.{PostRequest, WebsocketRequest}
import esw.http.core.commons.RouteHandlers
import mscoket.impl.HttpCodecs
import mscoket.impl.ws.WsServerFlow
import msocket.api.RequestHandler

class Routes(
    postHandler: RequestHandler[PostRequest, StandardRoute],
    websocketHandler: RequestHandler[WebsocketRequest, Source[Message, NotUsed]],
    routeHandlers: RouteHandlers
) extends RestlessCodecs
    with HttpCodecs {
  val route: Route = handleExceptions(routeHandlers.commonExceptionHandlers) {
    handleRejections(routeHandlers.jsonRejectionHandler) {
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

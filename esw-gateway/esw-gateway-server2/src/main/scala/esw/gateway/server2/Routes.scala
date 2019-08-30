package esw.gateway.server2

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives.{handleExceptions, handleRejections}
import akka.http.scaladsl.server.{Route, StandardRoute}
import akka.stream.scaladsl.Source
import esw.gateway.api.codecs.RestlessCodecs
import esw.gateway.api.messages.{PostRequest, WebsocketRequest}
import esw.http.core.commons.RouteHandlers
import mscoket.impl.{HttpCodecs, RoutesFactory}
import msocket.api.RequestHandler

class Routes(
    postHandler: RequestHandler[PostRequest, StandardRoute],
    websocketHandler: RequestHandler[WebsocketRequest, Source[Message, NotUsed]],
    routeHandlers: RouteHandlers
) extends RestlessCodecs
    with HttpCodecs {

  val route: Route = handleExceptions(routeHandlers.commonExceptionHandlers) {
    handleRejections(routeHandlers.jsonRejectionHandler) {
      new RoutesFactory(postHandler, websocketHandler).route
    }
  }
}

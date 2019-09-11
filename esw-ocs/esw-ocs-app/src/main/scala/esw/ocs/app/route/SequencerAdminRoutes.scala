package esw.ocs.app.route

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, StandardRoute}
import akka.stream.scaladsl.Source
import esw.ocs.api.codecs.SequencerAdminHttpCodecs
import esw.ocs.api.protocol.{SequencerAdminPostRequest, SequencerAdminWebsocketRequest}
import mscoket.impl.HttpCodecs
import mscoket.impl.ws.WsServerFlow
import msocket.api.RequestHandler

class SequencerAdminRoutes(
    postHandler: RequestHandler[SequencerAdminPostRequest, StandardRoute],
    websocketHandler: RequestHandler[SequencerAdminWebsocketRequest, Source[Message, NotUsed]]
) extends SequencerAdminHttpCodecs
    with HttpCodecs {

  val route: Route =
    post {
      path("post") {
        entity(as[SequencerAdminPostRequest])(postHandler.handle)
      }
    } ~
      get {
        path("websocket") {
          handleWebSocketMessages {
            new WsServerFlow(websocketHandler).flow
          }
        }
      }
}

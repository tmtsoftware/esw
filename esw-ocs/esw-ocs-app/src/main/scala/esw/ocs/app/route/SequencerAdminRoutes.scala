package esw.ocs.app.route

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, StandardRoute}
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import esw.ocs.api.codecs.SequencerAdminHttpCodecs
import esw.ocs.api.protocol.{SequencerAdminPostRequest, SequencerAdminWebsocketRequest}
import msocket.impl.post.ServerHttpCodecs
import msocket.impl.ws.WsServerFlow
import msocket.api.MessageHandler
import msocket.impl.Encoding

class SequencerAdminRoutes(
    postHandler: MessageHandler[SequencerAdminPostRequest, StandardRoute],
    websocketHandlerFactory: Encoding[_] => MessageHandler[SequencerAdminWebsocketRequest, Source[Message, NotUsed]]
)(implicit mat: Materializer)
    extends SequencerAdminHttpCodecs
    with ServerHttpCodecs {

  val route: Route = cors() {
    post {
      path("post-endpoint") {
        entity(as[SequencerAdminPostRequest])(postHandler.handle)
      }
    } ~
    get {
      path("websocket-endpoint") {
        handleWebSocketMessages {
          new WsServerFlow(websocketHandlerFactory).flow
        }
      }
    }
  }
}

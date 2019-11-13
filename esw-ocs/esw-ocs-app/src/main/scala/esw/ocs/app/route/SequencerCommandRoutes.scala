package esw.ocs.app.route

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, StandardRoute}
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import esw.ocs.api.codecs.SequencerHttpCodecs
import esw.ocs.api.protocol.{SequencerCommandPostRequest, SequencerCommandWebsocketRequest}
import msocket.api.MessageHandler
import msocket.impl.Encoding
import msocket.impl.post.ServerHttpCodecs
import msocket.impl.ws.WsServerFlow

class SequencerCommandRoutes(
    postHandler: MessageHandler[SequencerCommandPostRequest, StandardRoute],
    websocketHandlerFactory: Encoding[_] => MessageHandler[SequencerCommandWebsocketRequest, Source[Message, NotUsed]]
)(implicit mat: Materializer)
    extends SequencerHttpCodecs
    with ServerHttpCodecs {

  val route: Route = cors() {
    post {
      path("post-endpoint") {
        entity(as[SequencerCommandPostRequest])(postHandler.handle)
      }
    } ~ get {
      path("websocket-endpoint") {
        handleWebSocketMessages {
          new WsServerFlow(websocketHandlerFactory).flow
        }
      }
    }
  }
}

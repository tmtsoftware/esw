package esw.ocs.app.route

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, StandardRoute}
import akka.stream.Materializer
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import esw.ocs.api.codecs.SequencerHttpCodecs
import esw.ocs.api.protocol.SequencerAdminPostRequest
import msocket.api.MessageHandler
import msocket.impl.post.ServerHttpCodecs

class SequencerAdminRoutes(
    postHandler: MessageHandler[SequencerAdminPostRequest, StandardRoute]
)(implicit mat: Materializer)
    extends SequencerHttpCodecs
    with ServerHttpCodecs {

  val route: Route = cors() {
    post {
      path("post-endpoint") {
        entity(as[SequencerAdminPostRequest])(postHandler.handle)
      }
    }
  }
}

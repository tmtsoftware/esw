package esw.ocs.app.route

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, StandardRoute}
import esw.ocs.api.codecs.SequencerAdminHttpCodecs
import esw.ocs.api.request.SequencerAdminPostRequest
import mscoket.impl.HttpCodecs
import msocket.api.RequestHandler

class SequencerAdminRoutes(
    postHandler: RequestHandler[SequencerAdminPostRequest, StandardRoute]
) extends SequencerAdminHttpCodecs
    with HttpCodecs {

  val route: Route =
    post {
      path("post") {
        entity(as[SequencerAdminPostRequest])(postHandler.handle)
      }
    }
}

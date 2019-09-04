package esw.ocs.admin.server

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, StandardRoute}
import esw.http.core.commons.RouteHandlers
import esw.ocs.admin.api.{SequencerAdminHttpCodecs, SequencerAdminPostRequest}
import mscoket.impl.HttpCodecs
import msocket.api.RequestHandler

class Routes(
    postHandler: RequestHandler[SequencerAdminPostRequest, StandardRoute],
    routeHandlers: RouteHandlers
) extends SequencerAdminHttpCodecs
    with HttpCodecs {

  val route: Route = handleExceptions(routeHandlers.commonExceptionHandlers) {
    handleRejections(routeHandlers.jsonRejectionHandler) {
      post {
        path("post") {
          entity(as[SequencerAdminPostRequest])(postHandler.handle)
        }
      }
    }
  }
}

package esw.ocs.app

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import esw.http.core.commons.RouteHandlers
import esw.ocs.app.SequencerAdminPostRequest.GetSequence
import esw.ocs.client.SequencerAdminClient
import mscoket.impl.HttpCodecs

class Routes(
    sequencerAdminClient: SequencerAdminClient,
    routeHandlers: RouteHandlers
) extends SequencerAdminHttpCodecs
    with HttpCodecs {

  val route: Route = handleExceptions(routeHandlers.commonExceptionHandlers) {
    handleRejections(routeHandlers.jsonRejectionHandler) {
      post {
        path("post") {
          entity(as[SequencerAdminPostRequest]) {
            case GetSequence() => complete(sequencerAdminClient.getSequence)
          }
        }
      }
    }
  }
}

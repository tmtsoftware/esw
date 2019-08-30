package esw.gateway.server2

import akka.http.scaladsl.server.Directives.{handleExceptions, handleRejections}
import akka.http.scaladsl.server.Route
import esw.gateway.api.codecs.RestlessCodecs
import esw.http.core.commons.RouteHandlers
import mscoket.impl.HttpCodecs

class Routes(routes: Route, routeHandlers: RouteHandlers) extends RestlessCodecs with HttpCodecs {
  val route: Route = handleExceptions(routeHandlers.commonExceptionHandlers) {
    handleRejections(routeHandlers.jsonRejectionHandler) {
      routes
    }
  }
}

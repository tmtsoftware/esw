package esw.gateway.server

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import esw.gateway.server.routes.{CommandRoutes, EventRoutes}
import esw.template.http.server.CswContext

class Routes(cswCtx: CswContext) extends JsonSupportExt {
  val eventRoutes: Route   = new EventRoutes(cswCtx).route
  val commandRoutes: Route = new CommandRoutes(cswCtx).route

  def route: Route =
    handleExceptions(RouteExceptionHandlers.commonHandlers) {
      commandRoutes ~ eventRoutes
    }
}

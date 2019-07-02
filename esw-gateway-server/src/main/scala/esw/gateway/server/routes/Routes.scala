package esw.gateway.server.routes

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LoggingMagnet}
import akka.http.scaladsl.server.{Directive0, Route}
import esw.template.http.server.commons.RouteExceptionHandlers
import esw.template.http.server.csw.utils.CswContext

class Routes(cswCtx: CswContext) {

  private val logRequest: HttpRequest ⇒ Unit = req ⇒
    cswCtx.logger.info(
      "HTTP request received",
      Map(
        "url"     → req.uri.toString(),
        "method"  → req.method.value.toString,
        "headers" → req.headers.mkString(",")
      )
    )

  private val routeLogger: Directive0 = DebuggingDirectives.logRequest(LoggingMagnet(_ ⇒ logRequest))
  private val eventRoutes: Route      = new EventRoutes(cswCtx).route
  private val commandRoutes: Route    = new CommandRoutes(cswCtx).route

  def route: Route = routeLogger {
    handleExceptions(RouteExceptionHandlers.commonHandlers) {
      commandRoutes ~ eventRoutes
    }
  }
}

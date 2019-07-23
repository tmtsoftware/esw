package esw.gateway.server.routes

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LoggingMagnet}
import akka.http.scaladsl.server.{Directive0, Route}
import esw.http.core.utils.CswContext

class Routes(cswCtx: CswContext) {

  import cswCtx._

  private val logRequest: HttpRequest => Unit = req =>
    logger.info(
      "HTTP request received",
      Map(
        "url"     -> req.uri.toString(),
        "method"  -> req.method.value.toString,
        "headers" -> req.headers.mkString(",")
      )
    )

  private val routeLogger: Directive0 = DebuggingDirectives.logRequest(LoggingMagnet(_ => logRequest))
  private val eventRoutes: Route      = new EventRoutes(cswCtx).route
  private val commandRoutes: Route    = new CommandRoutes(cswCtx).route
  private val alarmRoutes: Route      = new AlarmRoutes(cswCtx).route

  def route: Route = routeLogger {
    handleExceptions(routeHandlers.commonExceptionHandlers) {
      handleRejections(routeHandlers.jsonRejectionHandler) {
        commandRoutes ~ eventRoutes ~ alarmRoutes
      }
    }
  }
}

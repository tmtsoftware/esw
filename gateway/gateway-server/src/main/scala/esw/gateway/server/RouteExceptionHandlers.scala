package esw.gateway.server

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, ExceptionHandler}
import csw.commons.http.{JsonRejectionHandler, JsonSupport}

import scala.concurrent.TimeoutException
import scala.util.control.NonFatal

object RouteExceptionHandlers extends Directives with JsonRejectionHandler {

  implicit def handlers: ExceptionHandler = ExceptionHandler {
    case ex: TimeoutException ⇒
      complete(JsonSupport.asJsonResponse(StatusCodes.GatewayTimeout, ex.getMessage))
    case NonFatal(ex) ⇒
      complete(JsonSupport.asJsonResponse(StatusCodes.InternalServerError, ex.getMessage))
  }
}

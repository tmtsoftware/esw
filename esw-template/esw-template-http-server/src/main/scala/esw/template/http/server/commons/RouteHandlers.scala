package esw.template.http.server.commons

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, ExceptionHandler}
import csw.commons.http.{JsonRejectionHandler, JsonSupport}

import scala.concurrent.TimeoutException
import scala.util.control.NonFatal

object RouteHandlers extends Directives with JsonRejectionHandler {

  implicit def commonExceptionHandlers: ExceptionHandler = ExceptionHandler {
    case ex: TimeoutException =>
      complete(JsonSupport.asJsonResponse(StatusCodes.GatewayTimeout, ex.getMessage))
    case NonFatal(ex) =>
      complete(JsonSupport.asJsonResponse(StatusCodes.InternalServerError, ex.getMessage))
  }
}

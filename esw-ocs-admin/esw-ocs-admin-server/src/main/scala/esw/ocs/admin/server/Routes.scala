package esw.ocs.admin.server

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route, StandardRoute}
import csw.logging.api.scaladsl.Logger
import esw.ocs.admin.api.{SequencerAdminHttpCodecs, SequencerAdminPostRequest}
import mscoket.impl.HttpCodecs
import msocket.api.RequestHandler

import scala.util.control.NonFatal

class Routes(
    postHandler: RequestHandler[SequencerAdminPostRequest, StandardRoute],
    log: Logger
) extends SequencerAdminHttpCodecs
    with HttpCodecs {

  val commonExceptionHandlers: ExceptionHandler = ExceptionHandler {
    case NonFatal(ex) =>
      log.error(ex.getMessage, ex = ex)
      complete(HttpResponse(StatusCodes.InternalServerError))
  }
  val route: Route = handleExceptions(commonExceptionHandlers) {
    handleRejections(RejectionHandler.default) {
      post {
        path("post") {
          entity(as[SequencerAdminPostRequest])(postHandler.handle)
        }
      }
    }
  }
}

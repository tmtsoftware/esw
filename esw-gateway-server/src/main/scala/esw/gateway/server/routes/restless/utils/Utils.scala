package esw.gateway.server.routes.restless.utils

import akka.stream.scaladsl.Source
import esw.gateway.server.routes.restless.messages.ErrorResponseMsg

import scala.concurrent.Future

object Utils {
  def emptySourceWithError(error: ErrorResponseMsg): Source[Nothing, Future[Some[ErrorResponseMsg]]] =
    Source.empty.mapMaterializedValue(_ => Future.successful(Some(error)))
}

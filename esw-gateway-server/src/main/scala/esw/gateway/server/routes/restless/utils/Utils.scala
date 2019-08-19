package esw.gateway.server.routes.restless.utils

import akka.stream.scaladsl.Source

import scala.concurrent.Future

object Utils {
  def emptySourceWithError[T](error: T): Source[Nothing, Future[Some[T]]] =
    Source.empty.mapMaterializedValue(_ => Future.successful(Some(error)))
}

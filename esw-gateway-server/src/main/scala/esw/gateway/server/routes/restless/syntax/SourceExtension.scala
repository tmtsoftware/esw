package esw.gateway.server.routes.restless.syntax

import akka.stream.scaladsl.Source

import scala.concurrent.Future

object SourceExtension {
  def emptyWithError[S, E](error: E): Source[S, Future[Some[E]]] =
    Source.empty.mapMaterializedValue(_ => Future.successful(Some(error)))
}

package esw.ocs.utils

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

object FutureEitherExt {
  implicit class RichFuture[T](future: Future[T]) {
    def toEither[E, V](error: Throwable => E)(implicit ec: ExecutionContext): Future[Either[E, T]] =
      future.map(Right(_)).recover { case NonFatal(ex) => Left(error(ex)) }
  }
}

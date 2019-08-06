package esw.ocs.syntax

import esw.ocs.internal.Timeouts

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.control.NonFatal

object FutureSyntax {
  implicit final class FutureOps[T](private val f: Future[T]) extends AnyVal {
    def block: T = Await.result(f, Timeouts.DefaultTimeout)

    def toEither[E](error: Throwable => E)(implicit ec: ExecutionContext): Future[Either[E, T]] =
      f.map(Right(_)).recover { case NonFatal(ex) => Left(error(ex)) }
  }
}

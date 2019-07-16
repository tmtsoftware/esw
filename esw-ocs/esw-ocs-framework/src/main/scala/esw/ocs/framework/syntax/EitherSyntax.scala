package esw.ocs.framework.syntax

import scala.concurrent.{ExecutionContext, Future}

object EitherSyntax {
  implicit final class EitherOps[A, B, C](private val eab: Either[A, B]) extends AnyVal {
    def traverse(f: B => Future[C])(implicit ec: ExecutionContext): Future[Either[A, C]] =
      eab match {
        case Right(b) => f(b).map(Right(_))
        case Left(a)  => Future.successful(Left(a))
      }
  }
}

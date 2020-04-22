package esw.commons.utils
import java.util.concurrent.CompletionStage

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.{ExecutionContext, Future}

object FutureEitherUtils {
  implicit class FutureEither[L <: Throwable, R](futureEither: Future[Either[L, R]]) {
    def right[S](f: R => S)(implicit executor: ExecutionContext): Future[Either[L, S]] = futureEither.map(_.map(f))

    def left(implicit executor: ExecutionContext): Future[Either.LeftProjection[L, R]] = futureEither.map(_.left)

    def flatRight[S](f: R => Future[Either[L, S]])(implicit executor: ExecutionContext): Future[Either[L, S]] =
      futureEither.flatMap {
        case Left(l)  => Future.successful(Left(l))
        case Right(r) => f(r)
      }

    def toJava(implicit executor: ExecutionContext): CompletionStage[R] =
      futureEither.map {
        case Left(error)  => throw error
        case Right(value) => value
      }.toJava

    def toJava[S](onSuccess: R => S)(implicit executor: ExecutionContext): CompletionStage[S] =
      futureEither.map {
        case Left(error)  => throw error
        case Right(value) => onSuccess(value)
      }.toJava
  }
}

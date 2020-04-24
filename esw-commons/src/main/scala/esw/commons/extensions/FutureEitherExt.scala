package esw.commons.extensions

import java.util.concurrent.CompletionStage

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

object FutureEitherExt {
  implicit class FutureEitherOps[+L <: Throwable, R](private val futureEither: Future[Either[L, R]]) extends AnyVal {
    def mapRight[S](f: R => S)(implicit executor: ExecutionContext): Future[Either[L, S]] = futureEither.map(_.map(f))

    def mapLeft[S](f: L => S)(implicit executor: ExecutionContext): Future[Either[S, R]] = futureEither.map(_.left.map(f))

    def mapError[L1 >: L](f: Throwable => L1)(implicit executor: ExecutionContext): Future[Either[L1, R]] = futureEither.recover {
      case NonFatal(e) => Left(f(e))
    }

    def flatMapRight[S](f: R => Future[S])(implicit executor: ExecutionContext): Future[Either[L, S]] =
      flatMapE(f(_).map(Right(_)))

    def flatMapE[S, L1 >: L](f: R => Future[Either[L1, S]])(implicit executor: ExecutionContext): Future[Either[L1, S]] =
      futureEither.flatMap {
        case Left(l)  => Future.successful(Left(l))
        case Right(r) => f(r)
      }

    def mapToAdt[S, R1 <: S, L1 <: S](rmap: R => R1, lmap: L => L1)(implicit ec: ExecutionContext): Future[S] =
      flatMapToAdt(r => Future.successful(rmap(r)), lmap)

    def flatMapToAdt[S, R1 <: S, L1 <: S](rmap: R => Future[R1], lmap: L => L1)(implicit ec: ExecutionContext): Future[S] =
      futureEither.flatMap {
        case Right(r) => rmap(r)
        case Left(l)  => Future.successful(lmap(l))
      }

    def toJava(implicit executor: ExecutionContext): CompletionStage[R] = toJava(onSuccess = identity)
    def toJava[S](onSuccess: R => S)(implicit executor: ExecutionContext): CompletionStage[S] =
      mapToAdt(onSuccess, throw _).toJava
  }
}

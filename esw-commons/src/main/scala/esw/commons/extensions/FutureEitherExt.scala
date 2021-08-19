package esw.commons.extensions

import java.util.concurrent.CompletionStage

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

/**
 * This is an extension class containing convenience functions for handling use cases involving Either & Future.
 */
object FutureEitherExt {
  implicit class FutureEitherOps[+L, R](private val futureEither: Future[Either[L, R]]) extends AnyVal {
    def mapRight[R1](f: R => R1)(implicit executor: ExecutionContext): Future[Either[L, R1]] = futureEither.map(_.map(f))
    def mapRightE[L1 >: L, R1](f: R => Either[L1, R1])(implicit executor: ExecutionContext): Future[Either[L1, R1]] =
      futureEither.flatMapE(r => Future.successful(f(r)))

    def mapLeft[L1](f: L => L1)(implicit executor: ExecutionContext): Future[Either[L1, R]] = futureEither.map(_.left.map(f))

    def mapError[L1 >: L](f: Throwable => L1)(implicit executor: ExecutionContext): Future[Either[L1, R]] =
      futureEither.recover { case NonFatal(e) => Left(f(e)) }

    def flatMapRight[R1](f: R => Future[R1])(implicit executor: ExecutionContext): Future[Either[L, R1]] =
      flatMapE(f(_).map(Right(_)))

    def flatMapE[L1 >: L, R1](f: R => Future[Either[L1, R1]])(implicit executor: ExecutionContext): Future[Either[L1, R1]] =
      futureEither.flatMap {
        case Left(l)  => Future.successful(Left(l))
        case Right(r) => f(r)
      }

    def mapToAdt[ADT, R1 <: ADT, L1 <: ADT](rmap: R => R1, lmap: L => L1)(implicit ec: ExecutionContext): Future[ADT] =
      flatMapToAdt(r => Future.successful(rmap(r)), lmap)

    def flatMapToAdt[ADT, R1 <: ADT, L1 <: ADT](rmap: R => Future[R1], lmap: L => L1)(implicit
        ec: ExecutionContext
    ): Future[ADT] =
      futureEither.flatMap {
        case Right(r) => rmap(r)
        case Left(l)  => Future.successful(lmap(l))
      }
  }

  implicit class FutureEitherJavaOps[+L <: Throwable, R](private val futureEither: Future[Either[L, R]]) extends AnyVal {
    def toJava(implicit executor: ExecutionContext): CompletionStage[R] = toJava(onSuccess = identity)
    def toJava[S](onSuccess: R => S)(implicit executor: ExecutionContext): CompletionStage[S] =
      futureEither.mapToAdt(onSuccess, throw _).toJava
  }

}

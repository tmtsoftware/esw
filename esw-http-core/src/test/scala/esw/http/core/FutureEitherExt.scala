package esw.http.core

import org.scalatest.EitherValues.convertLeftProjectionToValuable
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture

import scala.concurrent.duration.{Duration, DurationDouble}
import scala.concurrent.{Await, Future}

trait FutureEitherExt {

  val defaultTimeout: Duration = 10.seconds

  implicit class FutureOps[T](f: Future[T]) {
    def awaitResult: T = Await.result(f, defaultTimeout)
  }

  implicit class EitherOps[L, R](either: Either[L, R]) {
    def rightValue: R = either.toOption.get
    def leftValue: L  = either.left.value
  }

  implicit class FutureEitherOps[L, R](futureEither: Future[Either[L, R]]) {
    def rightValue: R = futureEither.futureValue.rightValue
    def leftValue: L  = futureEither.futureValue.leftValue
  }

}

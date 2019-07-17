package esw.ocs.framework

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest._
import org.scalatest.concurrent.{Eventually, ScalaFutures}

import scala.concurrent.duration.{Duration, DurationDouble}
import scala.concurrent.{Await, Future}

trait BaseTestSuite
    extends WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with ScalaFutures
    with OptionValues
    with EitherValues
    with TypeCheckedTripleEquals
    with Eventually {
  val defaultTimeout: Duration = 10.seconds

  implicit class FutureOps[T](f: Future[T]) {
    def await: T = Await.result(f, defaultTimeout)
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

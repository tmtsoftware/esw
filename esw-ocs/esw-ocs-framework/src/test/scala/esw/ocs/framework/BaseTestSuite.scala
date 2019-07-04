package esw.ocs.framework

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
    with Eventually {
  val defaultTimeout: Duration = 10.seconds

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds)

  implicit class FutureOps[T](f: Future[T]) {
    def await: T = Await.result(f, defaultTimeout)
  }

  implicit class FutureEitherOps[E, R](futureEither: Future[Either[E, R]]) {
    def rightValue: R = futureEither.futureValue.right.value
    def leftValue: E  = futureEither.futureValue.left.value
  }

}

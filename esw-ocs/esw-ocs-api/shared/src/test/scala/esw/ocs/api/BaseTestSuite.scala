package esw.ocs.api

import org.mockito.MockitoSugar
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest._
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Future

trait BaseTestSuite
    extends AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with ScalaFutures
    with OptionValues
    with EitherValues
    with MockitoSugar
    with TypeCheckedTripleEquals
    with Eventually {

  implicit class FutureEitherOps[L, R](futureEither: Future[Either[L, R]]) {
    def rightValue: R = futureEither.futureValue.toOption.get
    def leftValue: L  = futureEither.futureValue.left.value
  }

}

package esw.ocs.api

import org.mockito.MockitoSugar
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.SpanSugar.convertDoubleToGrainOfTime
import org.scalatest.wordspec.AnyWordSpecLike

trait BaseTestSuite
    extends AnyWordSpecLike
    with Matchers
    with ScalaFutures
    with TypeCheckedTripleEquals
    with BeforeAndAfterAll
    with MockitoSugar {
  override implicit def patienceConfig: PatienceConfig = PatienceConfig(5.seconds)
}

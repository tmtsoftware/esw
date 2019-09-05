package esw.http.core

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest._
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar

trait BaseTestSuite
    extends WordSpec
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with ScalaFutures
    with OptionValues
    with EitherValues
    with MockitoSugar
    with TypeCheckedTripleEquals
    with Eventually
    with FutureEitherExt

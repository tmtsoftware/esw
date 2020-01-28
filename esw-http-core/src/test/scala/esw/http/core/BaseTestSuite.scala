package esw.http.core

import org.mockito.MockitoSugar
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest._
import org.scalatest.concurrent.{Eventually, ScalaFutures}

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

package esw.agent.app

import org.mockito.MockitoSugar
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

trait BaseTestSuite
    extends AnyWordSpecLike
    with MockitoSugar
    with Matchers
    with TypeCheckedTripleEquals
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with Eventually
    with ScalaFutures

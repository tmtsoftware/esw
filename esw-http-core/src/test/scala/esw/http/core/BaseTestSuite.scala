package esw.http.core

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpec}

trait BaseTestSuite
    extends WordSpec
    with Matchers
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with MockitoSugar
    with ScalaFutures
    with ArgumentMatchersSugar

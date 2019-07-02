package esw.template.http.server

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpec}

trait BaseTestSuite
    extends WordSpec
    with Matchers
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with MockitoSugar
    with ArgumentMatchersSugar

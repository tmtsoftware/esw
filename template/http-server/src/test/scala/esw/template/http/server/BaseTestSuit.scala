package esw.template.http.server

import org.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

trait BaseTestSuit extends FunSuite with Matchers with BeforeAndAfterAll with MockitoSugar

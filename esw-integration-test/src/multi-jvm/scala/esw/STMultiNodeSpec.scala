package esw

import akka.remote.testkit.{MultiNodeSpec, MultiNodeSpecCallbacks}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers

/**
 * Hooks up MultiNodeSpec with ScalaTest
 */
trait STMultiNodeSpec extends MultiNodeSpecCallbacks with AnyFunSuiteLike with Matchers with BeforeAndAfterAll with ScalaFutures {
  self: MultiNodeSpec =>

  override def beforeAll(): Unit = multiNodeSpecBeforeAll()
  override def afterAll(): Unit  = multiNodeSpecAfterAll()
}

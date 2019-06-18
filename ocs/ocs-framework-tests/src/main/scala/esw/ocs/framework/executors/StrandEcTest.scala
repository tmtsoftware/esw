package esw.ocs.framework.executors

import org.scalatest.{FunSuite, Matchers}

class StrandEcTest extends FunSuite with Matchers {
  test("shutdown should stop executorService") {
    val strandEc = StrandEc()
    strandEc.shutdown()
    strandEc.executorService.isShutdown shouldBe true
  }
}

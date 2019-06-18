package esw.ocs.framework.executors

import esw.ocs.framework.BaseTestSuite

class StrandEcTest extends BaseTestSuite {

  "shutdown" should {
    "stop executor service" in {
      val strandEc = StrandEc()
      strandEc.shutdown()
      strandEc.executorService.isShutdown shouldBe true
    }
  }

}

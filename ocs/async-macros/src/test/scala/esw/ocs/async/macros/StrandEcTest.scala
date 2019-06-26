package esw.ocs.async.macros

import org.scalatest.{Matchers, WordSpec}

class StrandEcTest extends WordSpec with Matchers {
  "shutdown" must {
    "stop executor service" in {
      val strandEc = StrandEc()
      strandEc.shutdown()
      strandEc.executorService.isShutdown shouldBe true
    }
  }
}

package esw.ocs.dsl.script

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class StrandEcTest extends AnyWordSpec with Matchers {
  "shutdown" must {
    "stop executor service" in {
      val strandEc = StrandEc()
      strandEc.shutdown()
      strandEc.executorService.isShutdown shouldBe true
    }
  }
}

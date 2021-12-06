package esw.ocs.api.models

import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.IRIS
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpec

class ObsModeTest extends AnyWordSpec with Matchers {

  "ObsMode" must {
    "create obsMode from given prefix | ESW-561" in {
      val prefix1 = Prefix(IRIS, "IRIS_ImagerAndIFS.variation1")
      val prefix2 = Prefix(IRIS, "IRIS_ImagerAndIFS")
      ObsMode.from(prefix1) shouldBe ObsMode("IRIS_ImagerAndIFS")
      ObsMode.from(prefix2) shouldBe ObsMode("IRIS_ImagerAndIFS")
    }
  }
}

package esw.ocs.api.models

import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.IRIS
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpec

class VariationIdTest extends AnyWordSpec with Matchers {

  "VariationId" must {
    "create prefix from given obsMode | ESW-561" in {
      val variationId1     = VariationId(IRIS)
      val variationId2     = VariationId(IRIS, Some(Variation("variation1")))
      val variationId3     = VariationId(IRIS, Some(Variation("variation1.variation2")))
      val darkNightObsMode = ObsMode("darknight")
      variationId1.prefix(darkNightObsMode) shouldBe Prefix(IRIS, "darknight")
      variationId2.prefix(darkNightObsMode) shouldBe Prefix(IRIS, "darknight.variation1")
      variationId3.prefix(darkNightObsMode) shouldBe Prefix(IRIS, "darknight.variation1.variation2")
    }

    "create VariationId from variationIdString | ESW-561" in {
      val variationIdString1 = "IRIS"
      val variationIdString2 = "IRIS.variation1"
      val variationIdString3 = "IRIS.variation1.variation2"

      VariationId.from(variationIdString1) shouldBe VariationId(IRIS)
      VariationId.from(variationIdString2) shouldBe VariationId(IRIS, Some(Variation("variation1")))
      VariationId.from(variationIdString3) shouldBe VariationId(IRIS, Some(Variation("variation1.variation2")))
    }
  }
}

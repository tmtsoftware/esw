/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.api.models

import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.IRIS
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpec

class VariationInfoTest extends AnyWordSpec with Matchers {

  "VariationId" must {
    "create prefix from given obsMode | ESW-561" in {
      val variationInfo1   = VariationInfo(IRIS)
      val variationInfo2   = VariationInfo(IRIS, Some(Variation("variation1")))
      val variationInfo3   = VariationInfo(IRIS, Some(Variation("variation1.variation2")))
      val darkNightObsMode = ObsMode("darknight")
      variationInfo1.prefix(darkNightObsMode) shouldBe Prefix(IRIS, "darknight")
      variationInfo2.prefix(darkNightObsMode) shouldBe Prefix(IRIS, "darknight.variation1")
      variationInfo3.prefix(darkNightObsMode) shouldBe Prefix(IRIS, "darknight.variation1.variation2")
    }

    "create VariationId from variationInfoString | ESW-561" in {
      val variationInfoString1 = "IRIS"
      val variationInfoString2 = "IRIS.variation1"
      val variationInfoString3 = "IRIS.variation1.variation2"

      VariationInfo.from(variationInfoString1) shouldBe VariationInfo(IRIS)
      VariationInfo.from(variationInfoString2) shouldBe VariationInfo(IRIS, Some(Variation("variation1")))
      VariationInfo.from(variationInfoString3) shouldBe VariationInfo(IRIS, Some(Variation("variation1.variation2")))
    }
  }
}

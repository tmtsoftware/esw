package esw.ocs.api.models

import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.IRIS
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpec

class SequencerIdTest extends AnyWordSpec with Matchers {

  "SequencerId" must {
    "create prefix from given obsMode | ESW-561" in {
      val sequencerId1     = SequencerId(IRIS)
      val sequencerId2     = SequencerId(IRIS, Some("variation1"))
      val sequencerId3     = SequencerId(IRIS, Some("variation1.variation2"))
      val darkNightObsMode = ObsMode("darknight")
      sequencerId1.prefix(darkNightObsMode) shouldBe Prefix(IRIS, "darknight")
      sequencerId2.prefix(darkNightObsMode) shouldBe Prefix(IRIS, "darknight.variation1")
      sequencerId3.prefix(darkNightObsMode) shouldBe Prefix(IRIS, "darknight.variation1.variation2")
    }

    "create SequencerId from sequencerIdString | ESW-561" in {
      val sequencerIdString1 = "IRIS"
      val sequencerIdString2 = "IRIS.variation1"
      val sequencerIdString3 = "IRIS.variation1.variation2"

      SequencerId.fromString(sequencerIdString1) shouldBe SequencerId(IRIS)
      SequencerId.fromString(sequencerIdString2) shouldBe SequencerId(IRIS, Some("variation1"))
      SequencerId.fromString(sequencerIdString3) shouldBe SequencerId(IRIS, Some("variation1.variation2"))
    }
  }
}

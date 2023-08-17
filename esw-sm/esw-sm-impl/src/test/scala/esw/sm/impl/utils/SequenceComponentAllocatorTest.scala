package esw.sm.impl.utils

import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.PekkoConnection
import csw.location.api.models.{PekkoLocation, ComponentId, Metadata}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, IRIS, TCS}
import esw.ocs.api.models.{ObsMode, Variation, VariationInfo}
import esw.sm.api.models.VariationInfos
import esw.sm.api.protocol.StartSequencerResponse.SequenceComponentNotAvailable
import esw.testcommons.BaseTestSuite

import java.net.URI

class SequenceComponentAllocatorTest extends BaseTestSuite {
  val eswPrimarySeqCompLoc: PekkoLocation    = pekkoLocation(ComponentId(Prefix(ESW, "primary"), SequenceComponent))
  val eswSecondarySeqCompLoc: PekkoLocation  = pekkoLocation(ComponentId(Prefix(ESW, "secondary"), SequenceComponent))
  val tcsPrimarySeqCompLoc: PekkoLocation    = pekkoLocation(ComponentId(Prefix(TCS, "primary"), SequenceComponent))
  val irisPrimarySeqCompLoc: PekkoLocation   = pekkoLocation(ComponentId(Prefix(IRIS, "primary"), SequenceComponent))
  val clearSkies: ObsMode                    = ObsMode("clearSkies")
  private val eswVariationId: VariationInfo  = VariationInfo(ESW, Some(Variation("variation")))
  private val irisVariationId: VariationInfo = VariationInfo(IRIS, Some(Variation("variation")))
  private val tcsVariationId: VariationInfo  = VariationInfo(TCS, Some(Variation("variation")))
  val sequenceComponentAllocator             = new SequenceComponentAllocator()

  "allocate" must {
    "return mapping between provided sequencers and sequence components on first match basis | ESW-178, ESW-561" in {
      val sequencerToSeqCompMapping = sequenceComponentAllocator
        .allocate(
          List(tcsPrimarySeqCompLoc, irisPrimarySeqCompLoc, eswPrimarySeqCompLoc, eswSecondarySeqCompLoc),
          clearSkies,
          List(tcsVariationId, irisVariationId, eswVariationId)
        )
        .rightValue

      val expected =
        List(
          (eswVariationId, eswPrimarySeqCompLoc),
          (tcsVariationId, tcsPrimarySeqCompLoc),
          (irisVariationId, irisPrimarySeqCompLoc)
        )

      sequencerToSeqCompMapping should ===(expected)
    }

    "return mapping between provided sequencers and sequence components with ESW as fallback sequence component | ESW-178" in {
      val sequencerToSeqCompMapping = sequenceComponentAllocator
        .allocate(
          List(irisPrimarySeqCompLoc, eswPrimarySeqCompLoc, eswSecondarySeqCompLoc),
          clearSkies,
          List(tcsVariationId, irisVariationId, eswVariationId)
        )
        .rightValue

      val expected =
        List(
          (eswVariationId, eswPrimarySeqCompLoc),
          (tcsVariationId, eswSecondarySeqCompLoc),
          (irisVariationId, irisPrimarySeqCompLoc)
        )

      sequencerToSeqCompMapping should ===(expected)
    }

    "return SequenceComponentNotAvailable if no sequence component available for any provided sequencer | ESW-178, ESW-340, ESW-561" in {
      val sequencerToSeqCompMapping =
        sequenceComponentAllocator.allocate(
          List(tcsPrimarySeqCompLoc, eswPrimarySeqCompLoc),
          clearSkies,
          List(eswVariationId, tcsVariationId, irisVariationId)
        )

      val response = sequencerToSeqCompMapping.leftValue
      response shouldBe a[SequenceComponentNotAvailable]
      response.variationInfos shouldBe VariationInfos(irisVariationId) // because IRIS is last in the List.
    }
  }

  private def pekkoLocation(componentId: ComponentId): PekkoLocation =
    PekkoLocation(PekkoConnection(componentId), URI.create(""), Metadata.empty)
}

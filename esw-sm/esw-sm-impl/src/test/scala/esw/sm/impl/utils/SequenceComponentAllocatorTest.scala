package esw.sm.impl.utils

import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, Metadata}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, IRIS, TCS}
import esw.ocs.api.models.ObsMode
import esw.sm.api.protocol.StartSequencerResponse.SequenceComponentNotAvailable
import esw.testcommons.BaseTestSuite

import java.net.URI

class SequenceComponentAllocatorTest extends BaseTestSuite {
  val eswPrimarySeqCompLoc: AkkaLocation   = akkaLocation(ComponentId(Prefix(ESW, "primary"), SequenceComponent))
  val eswSecondarySeqCompLoc: AkkaLocation = akkaLocation(ComponentId(Prefix(ESW, "secondary"), SequenceComponent))
  val tcsPrimarySeqCompLoc: AkkaLocation   = akkaLocation(ComponentId(Prefix(TCS, "primary"), SequenceComponent))
  val irisPrimarySeqCompLoc: AkkaLocation  = akkaLocation(ComponentId(Prefix(IRIS, "primary"), SequenceComponent))
  val clearSkies                           = ObsMode("clearSkies")
  private val eswPrefix: Prefix            = Prefix(ESW, clearSkies.name)
  private val irisPrefix: Prefix           = Prefix(IRIS, clearSkies.name)
  private val tcsPrefix: Prefix            = Prefix(TCS, clearSkies.name)
  val sequenceComponentAllocator           = new SequenceComponentAllocator()

  "allocate" must {
    "return mapping between provided sequencers and sequence components on first match basis | ESW-178, ESW-561" in {
      val sequencerToSeqCompMapping = sequenceComponentAllocator
        .allocate(
          List(tcsPrimarySeqCompLoc, irisPrimarySeqCompLoc, eswPrimarySeqCompLoc, eswSecondarySeqCompLoc),
          List(tcsPrefix, irisPrefix, eswPrefix)
        )
        .rightValue

      val expected =
        List((eswPrefix, eswPrimarySeqCompLoc), (tcsPrefix, tcsPrimarySeqCompLoc), (irisPrefix, irisPrimarySeqCompLoc))

      sequencerToSeqCompMapping should ===(expected)
    }

    "return mapping between provided sequencers and sequence components with ESW as fallback sequence component | ESW-178" in {
      val sequencerToSeqCompMapping = sequenceComponentAllocator
        .allocate(
          List(irisPrimarySeqCompLoc, eswPrimarySeqCompLoc, eswSecondarySeqCompLoc),
          List(tcsPrefix, irisPrefix, eswPrefix)
        )
        .rightValue

      val expected =
        List((eswPrefix, eswPrimarySeqCompLoc), (tcsPrefix, eswSecondarySeqCompLoc), (irisPrefix, irisPrimarySeqCompLoc))

      sequencerToSeqCompMapping should ===(expected)
    }

    "return SequenceComponentNotAvailable if no sequence component available for any provided sequencer | ESW-178, ESW-340, ESW-561" in {
      val sequencerToSeqCompMapping =
        sequenceComponentAllocator.allocate(
          List(tcsPrimarySeqCompLoc, eswPrimarySeqCompLoc),
          List(eswPrefix, tcsPrefix, irisPrefix)
        )

      val response = sequencerToSeqCompMapping.leftValue
      response shouldBe a[SequenceComponentNotAvailable]
      response.sequencerPrefixes shouldBe List(irisPrefix) // because IRIS is last in the List.
    }
  }

  private def akkaLocation(componentId: ComponentId): AkkaLocation =
    AkkaLocation(AkkaConnection(componentId), URI.create(""), Metadata.empty)
}

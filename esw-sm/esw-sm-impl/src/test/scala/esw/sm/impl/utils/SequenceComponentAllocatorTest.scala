package esw.sm.impl.utils

import java.net.URI

import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, IRIS, TCS}
import esw.sm.api.protocol.StartSequencerResponse.SequenceComponentNotAvailable
import esw.sm.impl.config.Sequencers
import esw.testcommons.BaseTestSuite

class SequenceComponentAllocatorTest extends BaseTestSuite {
  val eswPrimarySeqCompLoc: AkkaLocation   = akkaLocation(ComponentId(Prefix(ESW, "primary"), SequenceComponent))
  val eswSecondarySeqCompLoc: AkkaLocation = akkaLocation(ComponentId(Prefix(ESW, "secondary"), SequenceComponent))
  val tcsPrimarySeqCompLoc: AkkaLocation   = akkaLocation(ComponentId(Prefix(TCS, "primary"), SequenceComponent))
  val irisPrimarySeqCompLoc: AkkaLocation  = akkaLocation(ComponentId(Prefix(IRIS, "primary"), SequenceComponent))

  val sequenceComponentAllocator = new SequenceComponentAllocator()

  "allocate" must {
    "return mapping between provided sequencers and sequence components on first match basis | ESW-178" in {
      val sequencerToSeqCompMapping = sequenceComponentAllocator
        .allocate(
          List(tcsPrimarySeqCompLoc, irisPrimarySeqCompLoc, eswPrimarySeqCompLoc, eswSecondarySeqCompLoc),
          Sequencers(TCS, IRIS, ESW)
        )
        .rightValue

      val expected = List((TCS, tcsPrimarySeqCompLoc), (IRIS, irisPrimarySeqCompLoc), (ESW, eswPrimarySeqCompLoc))

      sequencerToSeqCompMapping should ===(expected)
    }

    "return mapping between provided sequencers and sequence components with ESW as fallback sequence component | ESW-178" in {
      val sequencerToSeqCompMapping = sequenceComponentAllocator
        .allocate(
          List(irisPrimarySeqCompLoc, eswPrimarySeqCompLoc, eswSecondarySeqCompLoc),
          Sequencers(TCS, IRIS, ESW)
        )
        .rightValue

      val expected = List((TCS, eswPrimarySeqCompLoc), (IRIS, irisPrimarySeqCompLoc), (ESW, eswSecondarySeqCompLoc))

      sequencerToSeqCompMapping should ===(expected)
    }

    "return SequenceComponentNotAvailable if no sequence component available for any provided sequencer | ESW-178, ESW-340" in {
      val sequencerToSeqCompMapping =
        sequenceComponentAllocator.allocate(List(eswPrimarySeqCompLoc, tcsPrimarySeqCompLoc), Sequencers(ESW, TCS, IRIS))

      val response = sequencerToSeqCompMapping.leftValue
      response shouldBe a[SequenceComponentNotAvailable]
      response.subsystems shouldBe List(IRIS) // because IRIS is last in the List.
    }
  }

  private def akkaLocation(componentId: ComponentId): AkkaLocation = AkkaLocation(AkkaConnection(componentId), URI.create(""))
}

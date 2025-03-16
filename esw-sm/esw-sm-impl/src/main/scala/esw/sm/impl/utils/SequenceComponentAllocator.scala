package esw.sm.impl.utils

import csw.location.api.models.PekkoLocation
import csw.prefix.models.Subsystem
import csw.prefix.models.Subsystem.ESW
import esw.ocs.api.models.{ObsMode, VariationInfo}
import esw.sm.api.models.VariationInfos
import esw.sm.api.protocol.StartSequencerResponse.SequenceComponentNotAvailable
import esw.sm.impl.utils.Types.*

class SequenceComponentAllocator() {

  // map sequencers to available seq comp for subsystem or ESW (fallback)
  def allocate(
      sequenceComponents: List[SeqCompLocation],
      obsMode: ObsMode,
      variationInfos: List[VariationInfo]
  ): Either[SequenceComponentNotAvailable, List[(VariationInfo, SeqCompLocation)]] = {
    val partitionedSubsystems: List[VariationInfo] = {
      val partByESW = variationInfos.partition(p => p.subsystem == Subsystem.ESW)
      partByESW._1 ++ partByESW._2
    }
    var locations = sequenceComponents
    val mapping = for {
      sequencerId     <- partitionedSubsystems
      seqCompLocation <- findSeqComp(sequencerId.subsystem, locations)
    } yield {
      locations = locations.filterNot(_.equals(seqCompLocation))
      (sequencerId, seqCompLocation)
    }

    // check if each sequencer subsystem has allocated sequence component
    val diff = partitionedSubsystems.diff(mapping.map(_._1))
    if (diff.isEmpty) Right(mapping) else Left(SequenceComponentNotAvailable(VariationInfos(diff)))
  }

  // find sequence component for provided subsystem or ESW (fallback)
  private def findSeqComp(subsystem: Subsystem, seqCompLocations: List[SeqCompLocation]): Option[PekkoLocation] =
    seqCompLocations.find(_.prefix.subsystem == subsystem).orElse(seqCompLocations.find(_.prefix.subsystem == ESW))

}

object SequenceComponentAllocator {
  type SequencerToSequenceComponentMap = List[(VariationInfo, SeqCompLocation)]
}

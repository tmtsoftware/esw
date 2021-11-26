package esw.sm.impl.utils

import csw.location.api.models.AkkaLocation
import csw.prefix.models.Subsystem
import csw.prefix.models.Subsystem.ESW
import esw.sm.api.protocol.StartSequencerResponse.SequenceComponentNotAvailable
import esw.sm.impl.utils.Types.*

class SequenceComponentAllocator() {

  // map sequencers to available seq comp for subsystem or ESW (fallback)
  def allocate(
      sequenceComponents: List[SeqCompLocation],
      sequencerPrefixes: List[SequencerPrefix]
  ): Either[SequenceComponentNotAvailable, List[(SequencerPrefix, SeqCompLocation)]] = {
    val partitionedSubsystems: List[SequencerPrefix] = {
      val partByESW = sequencerPrefixes.partition(p => p.subsystem == Subsystem.ESW)
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
    if (diff.isEmpty) Right(mapping) else Left(SequenceComponentNotAvailable(diff))
  }

  // find sequence component for provided subsystem or ESW (fallback)
  private def findSeqComp(subsystem: Subsystem, seqCompLocations: List[SeqCompLocation]): Option[AkkaLocation] =
    seqCompLocations.find(_.prefix.subsystem == subsystem).orElse(seqCompLocations.find(_.prefix.subsystem == ESW))

}

object SequenceComponentAllocator {
  type SequencerToSequenceComponentMap = List[(SequencerPrefix, SeqCompLocation)]
}

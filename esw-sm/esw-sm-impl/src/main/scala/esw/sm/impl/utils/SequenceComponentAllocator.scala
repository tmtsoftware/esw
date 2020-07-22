package esw.sm.impl.utils

import csw.location.api.models.AkkaLocation
import csw.prefix.models.Subsystem
import csw.prefix.models.Subsystem.ESW
import esw.sm.api.protocol.StartSequencerResponse.SequenceComponentNotAvailable
import esw.sm.impl.config.Sequencers
import esw.sm.impl.utils.SequenceComponentAllocator.SequencerToSequenceComponentMap

class SequenceComponentAllocator() {

  // map sequencers to available seq comp for subsystem or ESW (fallback)
  def allocate(
      sequenceComponents: List[AkkaLocation],
      sequencers: Sequencers
  ): Either[SequenceComponentNotAvailable, SequencerToSequenceComponentMap] = {
    val subsystems = sequencers.subsystems
    var locations  = sequenceComponents
    val mapping = for {
      subsystem       <- subsystems
      seqCompLocation <- findSeqComp(subsystem, locations)
    } yield {
      locations = locations.filterNot(_.equals(seqCompLocation))
      (subsystem, seqCompLocation)
    }

    val diff = subsystems.diff(mapping.map(_._1))
    if (diff.isEmpty) Right(mapping) else Left(SequenceComponentNotAvailable(diff))
  }

  private def findSeqComp(subsystem: Subsystem, seqCompLocations: List[AkkaLocation]): Option[AkkaLocation] =
    seqCompLocations.find(_.prefix.subsystem == subsystem).orElse(seqCompLocations.find(_.prefix.subsystem == ESW))

}

object SequenceComponentAllocator {
  type SequencerToSequenceComponentMap = List[(Subsystem, AkkaLocation)]
}

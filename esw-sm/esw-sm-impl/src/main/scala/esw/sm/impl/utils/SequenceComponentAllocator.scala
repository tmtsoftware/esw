package esw.sm.impl.utils

import csw.location.api.models.AkkaLocation
import csw.prefix.models.Subsystem
import csw.prefix.models.Subsystem.ESW
import esw.sm.api.models.Sequencers
import esw.sm.api.protocol.StartSequencerResponse.SequenceComponentNotAvailable
import esw.sm.impl.utils.Types._

class SequenceComponentAllocator() {

  // map sequencers to available seq comp for subsystem or ESW (fallback)
  def allocate(
      sequenceComponents: List[SeqCompLocation],
      sequencers: Sequencers
  ): Either[SequenceComponentNotAvailable, List[(Subsystem, SeqCompLocation)]] = {
    val subsystems = {
      val partByESW = sequencers.subsystems.partition(_ == Subsystem.ESW)
      partByESW._1 ++ partByESW._2
    }
    var locations = sequenceComponents
    val mapping = for {
      subsystem       <- subsystems
      seqCompLocation <- findSeqComp(subsystem, locations)
    } yield {
      locations = locations.filterNot(_.equals(seqCompLocation))
      (subsystem, seqCompLocation)
    }

    // check if each sequencer subsystem has allocated sequence component
    val diff = subsystems.diff(mapping.map(_._1))
    if (diff.isEmpty) Right(mapping) else Left(SequenceComponentNotAvailable(diff))
  }

  // find sequence component for provided subsystem or ESW (fallback)
  private def findSeqComp(subsystem: Subsystem, seqCompLocations: List[SeqCompLocation]): Option[AkkaLocation] =
    seqCompLocations.find(_.prefix.subsystem == subsystem).orElse(seqCompLocations.find(_.prefix.subsystem == ESW))

}

object SequenceComponentAllocator {
  type SequencerToSequenceComponentMap = List[(Subsystem, SeqCompLocation)]
}

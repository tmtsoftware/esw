package esw.sm.api.protocol

import csw.prefix.models.Subsystem
import esw.ocs.api.models.ObsMode

sealed trait ShutdownSequencersPolicy
object ShutdownSequencersPolicy {
  case class SingleSequencer(subsystem: Subsystem, obsMode: ObsMode) extends ShutdownSequencersPolicy
  case class SubsystemSequencers(subsystem: Subsystem)               extends ShutdownSequencersPolicy
  case class ObsModeSequencers(obsMode: ObsMode)                     extends ShutdownSequencersPolicy
  case object AllSequencers                                          extends ShutdownSequencersPolicy
}

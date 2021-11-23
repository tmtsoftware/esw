package esw.sm.api.models

import csw.prefix.models.Prefix
import esw.ocs.api.models.ObsMode

case class ObsModeDetails(obsMode: ObsMode, status: ObsModeStatus, resources: Resources, sequencers: Sequencers)

sealed trait ObsModeStatus

object ObsModeStatus {
  case object Configured                                              extends ObsModeStatus
  case object Configurable                                            extends ObsModeStatus
  case class NonConfigurable(missingSequenceComponents: List[Prefix]) extends ObsModeStatus
}

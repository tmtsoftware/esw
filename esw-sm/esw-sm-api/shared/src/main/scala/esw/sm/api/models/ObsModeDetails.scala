package esw.sm.api.models

import esw.ocs.api.models.ObsMode

case class ObsModeDetails(obsMode: ObsMode, status: ObsModeStatus, resources: Resources, sequencers: VariationIds)

sealed trait ObsModeStatus

object ObsModeStatus {
  case object Configured                                                 extends ObsModeStatus
  case object Configurable                                               extends ObsModeStatus
  case class NonConfigurable(missingSequenceComponentsFor: VariationIds) extends ObsModeStatus
}

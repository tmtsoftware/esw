package esw.sm.api.models

import esw.ocs.api.models.ObsMode

case class ObsModeDetails(obsMode: ObsMode, status: ObsModeStatus, resources: Resources, sequencers: Sequencers)

sealed trait ObsModeStatus

object ObsModeStatus {
  case object Configured      extends ObsModeStatus
  case object Configurable    extends ObsModeStatus
  case object NonConfigurable extends ObsModeStatus
}

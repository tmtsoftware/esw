package esw.ocs.api.models

case class ObsModeWithStatus(obsMode: ObsMode, status: ObsModeStatus)

sealed trait ObsModeStatus

object ObsModeStatus {
  case object Configured      extends ObsModeStatus
  case object Configurable    extends ObsModeStatus
  case object NonConfigurable extends ObsModeStatus
}

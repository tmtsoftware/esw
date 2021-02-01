package esw.ocs.api.models

case class ObsModeWithStatus(obsMode: ObsMode, status: ObsModeStatus)

sealed trait ObsModeStatus

object ObsModeStatus {
  case object Running         extends ObsModeStatus
  case object Configurable    extends ObsModeStatus
  case object NonConfigurable extends ObsModeStatus
}

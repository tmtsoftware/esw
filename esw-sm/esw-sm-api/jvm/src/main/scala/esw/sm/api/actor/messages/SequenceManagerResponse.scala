package esw.sm.api.actor.messages

import csw.location.api.models.HttpLocation

sealed trait ConfigureResponse

object ConfigureResponse {
  case class Success(sequencerLocation: HttpLocation) extends ConfigureResponse

  sealed trait Failure                                     extends ConfigureResponse
  case object ConflictingResourcesWithRunningObsMode       extends Failure // todo : add conflicting obs mode
  case class FailedToStartSequencers(reasons: Set[String]) extends Failure
  case class ConfigurationFailure(msg: String)             extends Failure
}

sealed trait GetRunningObsModesResponse

object GetRunningObsModesResponse {
  case class Success(runningObsModes: Set[String]) extends GetRunningObsModesResponse
  case class Failed(msg: String)                   extends GetRunningObsModesResponse
}

sealed trait CleanupResponse

object CleanupResponse {
  case object Success            extends CleanupResponse
  case class Failed(msg: String) extends CleanupResponse
}

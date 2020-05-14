package esw.sm.api.models

import csw.location.api.models.HttpLocation
import esw.sm.api.codecs.SmAkkaSerializable

sealed trait ConfigureResponse extends SmAkkaSerializable

object ConfigureResponse {
  case class Success(sequencerLocation: HttpLocation) extends ConfigureResponse

  sealed trait Failure                                                           extends ConfigureResponse
  case class ConflictingResourcesWithRunningObsMode(runningObsMode: Set[String]) extends Failure
  case class FailedToStartSequencers(reasons: Set[String])                       extends Failure
}

sealed trait GetRunningObsModesResponse extends SmAkkaSerializable

object GetRunningObsModesResponse {
  case class Success(runningObsModes: Set[String]) extends GetRunningObsModesResponse
  case class Failed(msg: String)                   extends GetRunningObsModesResponse
}

sealed trait CleanupResponse extends SmAkkaSerializable

object CleanupResponse {
  case object Success extends CleanupResponse

  sealed trait Failure extends CleanupResponse
}

sealed trait CommonFailure extends ConfigureResponse.Failure with CleanupResponse.Failure

object CommonFailure {
  case class LocationServiceError(msg: String)     extends CommonFailure
  case class ConfigurationMissing(obsMode: String) extends CommonFailure
}

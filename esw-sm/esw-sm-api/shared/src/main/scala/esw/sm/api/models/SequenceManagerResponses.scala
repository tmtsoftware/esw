package esw.sm.api.models

import csw.location.api.models.ComponentId
import esw.sm.api.codecs.SmAkkaSerializable

sealed trait ConfigureResponse extends SmAkkaSerializable

object ConfigureResponse {
  case class Success(masterSequencerComponentId: ComponentId) extends ConfigureResponse

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

sealed trait StartSequencerResponse extends SmAkkaSerializable

object StartSequencerResponse {
  sealed trait Success                                extends StartSequencerResponse
  case class Started(componentId: ComponentId)        extends Success
  case class AlreadyRunning(componentId: ComponentId) extends Success

  sealed trait Failure extends StartSequencerResponse
}

sealed trait ShutdownSequencerResponse extends SmAkkaSerializable

object ShutdownSequencerResponse {
  case object Success extends ShutdownSequencerResponse

  sealed trait Failure extends ShutdownSequencerResponse
}

sealed trait CommonFailure extends ConfigureResponse.Failure with CleanupResponse.Failure

object CommonFailure {
  case class LocationServiceError(msg: String)     extends AgentError with CommonFailure with ShutdownSequencerResponse.Failure
  case class ConfigurationMissing(obsMode: String) extends CommonFailure
}

sealed trait SequencerError extends Throwable with Product with StartSequencerResponse.Failure {
  def msg: String
}

sealed trait AgentError extends SequencerError

object SequenceManagerError {
  case class SpawnSequenceComponentFailed(msg: String) extends AgentError

  case class LoadScriptError(msg: String) extends SequencerError
}

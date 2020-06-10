package esw.sm.api.models

import csw.location.api.models.ComponentId
import csw.prefix.models.Prefix
import esw.sm.api.codecs.SmAkkaSerializable
import esw.sm.api.models.SequenceManagerError.UnloadScriptError

sealed trait ConfigureResponse extends SmAkkaSerializable

object ConfigureResponse {
  case class Success(masterSequencerComponentId: ComponentId) extends ConfigureResponse

  sealed trait Failure                                                           extends Throwable with ConfigureResponse
  case class ConflictingResourcesWithRunningObsMode(runningObsMode: Set[String]) extends Failure
  case class FailedToStartSequencers(reasons: Set[String])                       extends Failure
}

sealed trait GetRunningObsModesResponse extends SmAkkaSerializable

object GetRunningObsModesResponse {
  case class Success(runningObsModes: Set[String]) extends GetRunningObsModesResponse
  case class Failed(msg: String)                   extends Throwable with GetRunningObsModesResponse
}

sealed trait CleanupResponse extends SmAkkaSerializable

object CleanupResponse {
  case object Success extends CleanupResponse

  sealed trait Failure                                         extends Throwable with CleanupResponse
  case class FailedToShutdownSequencers(response: Set[String]) extends Failure
}

sealed trait StartSequencerResponse extends SmAkkaSerializable

object StartSequencerResponse {
  sealed trait Success                                extends StartSequencerResponse
  case class Started(componentId: ComponentId)        extends Success
  case class AlreadyRunning(componentId: ComponentId) extends Success

  sealed trait Failure extends Throwable with StartSequencerResponse with RestartSequencerResponse.Failure
}

sealed trait ShutdownSequencerResponse extends SmAkkaSerializable

object ShutdownSequencerResponse {
  case object Success extends ShutdownSequencerResponse

  sealed trait Failure extends Throwable with ShutdownSequencerResponse with RestartSequencerResponse.Failure {
    def msg: String
  }
}

sealed trait RestartSequencerResponse extends SmAkkaSerializable

object RestartSequencerResponse {
  case class Success(componentId: ComponentId) extends RestartSequencerResponse

  sealed trait Failure extends Throwable with RestartSequencerResponse {
    def msg: String
  }
}

sealed trait ShutdownAllSequencersResponse extends SmAkkaSerializable
object ShutdownAllSequencersResponse {
  case object Success extends ShutdownAllSequencersResponse

  sealed trait Failure                                                  extends Throwable with ShutdownAllSequencersResponse
  case class ShutdownFailure(failureResponses: List[UnloadScriptError]) extends ShutdownAllSequencersResponse.Failure
}

sealed trait CommonFailure extends Throwable with ConfigureResponse.Failure with CleanupResponse.Failure

object CommonFailure {
  case class ConfigurationMissing(obsMode: String) extends CommonFailure
  case class LocationServiceError(msg: String)
      extends AgentError
      with CommonFailure
      with ShutdownSequencerResponse.Failure
      with ShutdownAllSequencersResponse.Failure
}

sealed trait SequencerError extends Throwable with Product with StartSequencerResponse.Failure {
  def msg: String
}

sealed trait AgentError extends SequencerError

object SequenceManagerError {
  case class SpawnSequenceComponentFailed(msg: String) extends AgentError

  case class LoadScriptError(msg: String)                   extends SequencerError
  case class UnloadScriptError(prefix: Prefix, msg: String) extends SequencerError with ShutdownSequencerResponse.Failure
}

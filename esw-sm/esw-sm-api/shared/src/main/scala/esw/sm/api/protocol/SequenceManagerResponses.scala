package esw.sm.api.protocol

import csw.location.api.models.ComponentId
import esw.ocs.api.models.ObsMode
import esw.sm.api.codecs.SmAkkaSerializable

private[protocol] sealed trait SmFailure extends Throwable

sealed trait SmResponse extends SmAkkaSerializable

sealed trait ConfigureResponse extends SmResponse

object ConfigureResponse {
  case class Success(masterSequencerComponentId: ComponentId) extends ConfigureResponse

  sealed trait Failure                                                            extends SmFailure with ConfigureResponse
  case class ConflictingResourcesWithRunningObsMode(runningObsMode: Set[ObsMode]) extends Failure
  case class FailedToStartSequencers(reasons: Set[String])                        extends Failure
}

sealed trait GetRunningObsModesResponse extends SmResponse

object GetRunningObsModesResponse {
  case class Success(runningObsModes: Set[ObsMode]) extends GetRunningObsModesResponse
  case class Failed(msg: String)                    extends SmFailure with GetRunningObsModesResponse
}

sealed trait StartSequencerResponse extends SmResponse

object StartSequencerResponse {
  sealed trait Success                                extends StartSequencerResponse
  case class Started(componentId: ComponentId)        extends Success
  case class AlreadyRunning(componentId: ComponentId) extends Success

  sealed trait Failure extends SmFailure with StartSequencerResponse {
    def msg: String
  }
  case class LoadScriptError(msg: String)               extends Failure with RestartSequencerResponse.Failure
  case class SequenceComponentNotAvailable(msg: String) extends Failure
}

sealed trait ShutdownSequencersResponse extends SmResponse
object ShutdownSequencersResponse {
  case object Success  extends ShutdownSequencersResponse
  sealed trait Failure extends SmFailure with ShutdownSequencersResponse
}

sealed trait RestartSequencerResponse extends SmResponse

object RestartSequencerResponse {
  case class Success(componentId: ComponentId) extends RestartSequencerResponse

  sealed trait Failure extends SmFailure with RestartSequencerResponse {
    def msg: String
  }
}

sealed trait SpawnSequenceComponentResponse extends SmResponse

object SpawnSequenceComponentResponse {
  case class Success(componentId: ComponentId) extends SpawnSequenceComponentResponse

  sealed trait Failure extends SmFailure with SpawnSequenceComponentResponse
}

sealed trait ShutdownSequenceComponentResponse extends SmResponse
object ShutdownSequenceComponentResponse {
  case object Success extends ShutdownSequenceComponentResponse

  sealed trait Failure extends SmFailure with ShutdownSequenceComponentResponse
}

sealed trait CommonFailure extends SmFailure with ConfigureResponse.Failure

object CommonFailure {
  case class ConfigurationMissing(obsMode: ObsMode) extends CommonFailure
  case class LocationServiceError(msg: String)
      extends AgentError
      with CommonFailure
      with StartSequencerResponse.Failure
      with RestartSequencerResponse.Failure
      with ShutdownSequencersResponse.Failure
      with ShutdownSequenceComponentResponse.Failure
}

sealed trait AgentError extends SpawnSequenceComponentResponse.Failure

object AgentError {
  case class SpawnSequenceComponentFailed(msg: String) extends AgentError
}

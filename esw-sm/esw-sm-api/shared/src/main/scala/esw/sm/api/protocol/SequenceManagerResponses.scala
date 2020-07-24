package esw.sm.api.protocol

import csw.location.api.models.ComponentId
import csw.prefix.models.Subsystem
import esw.ocs.api.models.ObsMode
import esw.sm.api.codecs.SmAkkaSerializable
import esw.sm.api.models.AgentStatusResponses.AgentSeqCompsStatus

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
  case class LoadScriptError(msg: String) extends Failure with RestartSequencerResponse.Failure

  case class SequenceComponentNotAvailable(subsystems: List[Subsystem]) extends Failure with ConfigureResponse.Failure {
    override val msg: String = s"No sequence components found for subsystems : $subsystems"
  }
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

  sealed trait Failure                                 extends SmFailure with SpawnSequenceComponentResponse
  case class SpawnSequenceComponentFailed(msg: String) extends Failure
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
      extends CommonFailure
      with StartSequencerResponse.Failure
      with RestartSequencerResponse.Failure
      with ShutdownSequencersResponse.Failure
      with ShutdownSequenceComponentResponse.Failure
      with SpawnSequenceComponentResponse.Failure
      with ProvisionResponse.Failure
      with AgentStatusResponse.Failure
}

sealed trait ProvisionResponse extends SmResponse

object ProvisionResponse {
  case object Success extends ProvisionResponse

  sealed trait Failure                                                        extends SmFailure with ProvisionResponse
  case class NoMachineFoundForSubsystems(subsystems: Set[Subsystem])          extends Failure
  case class ConfigurationFailure(msg: String)                                extends Failure
  case class SpawningSequenceComponentsFailed(failureResponses: List[String]) extends Failure
}

sealed trait AgentStatusResponse extends SmResponse

object AgentStatusResponse {
  case class Success(response: List[AgentSeqCompsStatus]) extends AgentStatusResponse

  sealed trait Failure extends SmFailure with AgentStatusResponse
}

final case class Unhandled(state: String, messageType: String, msg: String)
    extends ConfigureResponse
    with StartSequencerResponse
    with RestartSequencerResponse
    with ShutdownSequencersResponse
    with ShutdownSequenceComponentResponse
    with SpawnSequenceComponentResponse
    with ProvisionResponse
    with AgentStatusResponse

object Unhandled {
  def apply(state: String, messageType: String): Unhandled =
    new Unhandled(state, messageType, s"Sequence Manager can not accept '$messageType' message in '${state}'")
}

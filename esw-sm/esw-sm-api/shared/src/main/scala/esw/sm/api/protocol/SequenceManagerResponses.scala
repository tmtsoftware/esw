package esw.sm.api.protocol

import csw.location.api.models.ComponentId
import csw.prefix.models.Prefix
import esw.ocs.api.models.ObsMode
import esw.sm.api.codecs.SmAkkaSerializable
import esw.sm.api.models.*

//Response models for the sequence manager(both http and actor)
sealed trait SmResponse extends SmAkkaSerializable

sealed trait SmFailure extends Exception with SmResponse {
  def msg: String
  override def getMessage: String = msg
}
case class FailedResponse(reason: String)
    extends SmFailure
    with ProvisionResponse.Failure
    with ConfigureResponse.Failure
    with StartSequencerResponse.Failure
    with ShutdownSequencersResponse.Failure
    with RestartSequencerResponse.Failure
    with ShutdownSequenceComponentResponse.Failure {
  override def msg: String = reason
}

sealed trait ConfigureResponse extends SmResponse

object ConfigureResponse {
  case class Success(masterSequencerComponentId: ComponentId) extends ConfigureResponse

  sealed trait Failure extends SmFailure with ConfigureResponse
  case class ConflictingResourcesWithRunningObsMode(runningObsMode: Set[ObsMode]) extends Failure {
    override def msg: String = s"Failed to configure : conflicting resources with $runningObsMode"
  }
  case class FailedToStartSequencers(reasons: Set[String]) extends Failure {
    override def msg: String = s"Failed to configure: failed to start sequencers $reasons"
  }
  case class ConfigurationMissing(obsMode: ObsMode) extends CommonFailure {
    override def msg: String = s"Failed to configure: configuration missing for $obsMode"
  }
}

sealed trait ObsModesDetailsResponse extends SmResponse

object ObsModesDetailsResponse {
  case class Success(obsModes: Set[ObsModeDetails]) extends ObsModesDetailsResponse

  sealed trait Failure extends SmFailure with ObsModesDetailsResponse

}
sealed trait StartSequencerResponse extends SmResponse

object StartSequencerResponse {
  sealed trait Success                                extends StartSequencerResponse
  case class Started(componentId: ComponentId)        extends Success
  case class AlreadyRunning(componentId: ComponentId) extends Success

  sealed trait Failure extends SmFailure with StartSequencerResponse

  case class LoadScriptError(reason: String) extends Failure with RestartSequencerResponse.Failure {
    override def msg: String = s"Failed to load sequencer script: $reason"
  }

  case class SequenceComponentNotAvailable private[sm] (variationInfos: VariationInfos, msg: String)
      extends Failure
      with ConfigureResponse.Failure

  object SequenceComponentNotAvailable {
    def apply(variationInfos: VariationInfos): SequenceComponentNotAvailable =
      new SequenceComponentNotAvailable(
        variationInfos,
        s"No sequence components found for sequencers : ${variationInfos.variationInfos.map(_.toString).mkString(",")}"
      )
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

  sealed trait Failure extends SmFailure with RestartSequencerResponse
}

sealed trait ShutdownSequenceComponentResponse extends SmResponse
object ShutdownSequenceComponentResponse {
  case object Success extends ShutdownSequenceComponentResponse

  sealed trait Failure extends SmFailure with ShutdownSequenceComponentResponse
}

// todo refactor this
sealed trait CommonFailure extends SmFailure with ConfigureResponse.Failure

object CommonFailure {
  case class LocationServiceError(reason: String)
      extends CommonFailure
      with StartSequencerResponse.Failure
      with RestartSequencerResponse.Failure
      with ShutdownSequencersResponse.Failure
      with ShutdownSequenceComponentResponse.Failure
      with ProvisionResponse.Failure
      with ObsModesDetailsResponse.Failure {
    override def msg: String = s"Failed with location service error: $reason"
  }
}

sealed trait ProvisionResponse extends SmResponse

object ProvisionResponse {
  case object Success extends ProvisionResponse

  sealed trait Failure extends SmFailure with ProvisionResponse
  case class CouldNotFindMachines(prefix: Set[Prefix]) extends Failure {
    override def msg: String = s"Failed to provision: could not find machines with $prefix to provision sequence components"
  }
  case class SpawningSequenceComponentsFailed(failureResponses: List[String]) extends Failure {
    override def msg: String = s"Failed to provision: spawning sequence components failed with $failureResponses"
  }
  case class ProvisionVersionFailure(reason: String) extends Failure {
    override def msg: String = s"Failed to provision: error in fetching version $reason"
  }
}

final case class Unhandled(state: String, messageType: String, msg: String)
    extends ConfigureResponse.Failure
    with StartSequencerResponse.Failure
    with RestartSequencerResponse.Failure
    with ShutdownSequencersResponse.Failure
    with ShutdownSequenceComponentResponse.Failure
    with ProvisionResponse.Failure

object Unhandled {
  def apply(state: String, messageType: String): Unhandled =
    new Unhandled(state, messageType, s"Sequence Manager can not accept '$messageType' message in '$state'")
}

// Model for status of a single resource
case class ResourceStatusResponse(
    resource: Resource,
    status: ResourceStatus,
    obsMode: Option[ObsMode] = None
)

object ResourceStatusResponse {
  def apply(resource: Resource): ResourceStatusResponse = ResourceStatusResponse(resource, ResourceStatus.Available)
}

// Model for status of multiple resources
sealed trait ResourcesStatusResponse extends SmResponse
object ResourcesStatusResponse {

  case class Success(resourcesStatus: List[ResourceStatusResponse]) extends ResourcesStatusResponse

  case class Failed(msg: String) extends SmFailure with ResourcesStatusResponse {
    override def getMessage: String = msg
  }
}

package esw.contract.data.sequencemanager

import csw.contract.ResourceFetcher
import csw.contract.generator.ClassNameHelpers._
import csw.contract.generator._
import csw.prefix.models.Subsystem
import esw.sm.api.codecs.SequenceManagerServiceCodecs
import esw.sm.api.models.{ObsModeDetails, ObsModeStatus, Resource, ResourceStatus}
import esw.sm.api.protocol.SequenceManagerRequest._
import esw.sm.api.protocol._

// ESW-355 Contract samples for sequence manager service. These samples are also used in `RoundTripTest`
object SequenceManagerContract extends SequenceManagerServiceCodecs with SequenceManagerData {

  private val models: ModelSet = ModelSet.models(
    ModelType[ConfigureResponse](
      configureSuccess,
      configurationMissing,
      conflictingResourcesWithRunningObsMode,
      failedToStartSequencers,
      locationServiceError,
      sequenceComponentNotAvailable,
      unhandled
    ),
    ModelType[ProvisionResponse](provisionSuccess, couldNotFindMachines, spawningSequenceComponentsFailed, unhandled),
    ModelType[ObsModeDetails](configuredObsMode),
    ModelType[ObsModeStatus](ObsModeStatus.Configurable, ObsModeStatus.Configured, ObsModeStatus.NonConfigurable),
    ModelType[ObsModesDetailsResponse](ObsModesDetailsSuccess, locationServiceError),
    ModelType[StartSequencerResponse](
      alreadyRunning,
      started,
      loadScriptError,
      sequenceComponentNotAvailable,
      locationServiceError,
      unhandled
    ),
    ModelType[RestartSequencerResponse](restartSequencerSuccess, loadScriptError, locationServiceError, unhandled),
    ModelType[ShutdownSequencersResponse](shutdownSequencerSuccess, locationServiceError, unhandled),
    ModelType[ShutdownSequenceComponentResponse](shutdownSequenceComponentSuccess, locationServiceError, unhandled),
    ModelType[AgentStatusResponse](agentStatusSuccess, locationServiceError, unhandled),
    ModelType(sequencerPrefix),
    ModelType(obsMode),
    ModelType(Subsystem),
    ModelType(provisionConfig),
    ModelType[ResourcesStatusResponse](resourcesStatusSuccess, resourcesStatusFailed),
    ModelType[Resource](irisResource, tcsResource),
    ModelType[ResourceStatus](ResourceStatus.InUse, ResourceStatus.Available)
  )

  private val httpRequests = new RequestSet[SequenceManagerRequest] {
    requestType(configure)
    requestType(provision)
    requestType(getObsModesDetails)
    requestType(startSequencer)
    requestType(restartSequencer)
    requestType(shutdownSequencer)
    requestType(shutdownSubsystemSequencers)
    requestType(shutdownObsModeSequencers)
    requestType(shutdownAllSequencers)
    requestType(shutdownSequenceComponent)
    requestType(shutdownAllSequenceComponents)
    requestType(getAgentStatus)
    requestType(getResourcesStatus)
  }

  private val httpEndpoints: List[Endpoint] = List(
    Endpoint(name[Configure], name[ConfigureResponse]),
    Endpoint(name[Provision], name[ProvisionResponse]),
    Endpoint(objectName(GetObsModesDetails), name[ObsModesDetailsResponse]),
    Endpoint(name[StartSequencer], name[StartSequencerResponse]),
    Endpoint(name[RestartSequencer], name[RestartSequencerResponse]),
    Endpoint(name[ShutdownSequencer], name[ShutdownSequencersResponse]),
    Endpoint(name[ShutdownSubsystemSequencers], name[ShutdownSequencersResponse]),
    Endpoint(name[ShutdownObsModeSequencers], name[ShutdownSequencersResponse]),
    Endpoint(objectName(ShutdownAllSequencers), name[ShutdownSequencersResponse]),
    Endpoint(name[ShutdownSequenceComponent], name[ShutdownSequenceComponentResponse]),
    Endpoint(objectName(ShutdownAllSequenceComponents), name[ShutdownSequenceComponentResponse]),
    Endpoint(objectName(GetAgentStatus), name[AgentStatusResponse]),
    Endpoint(objectName(GetResources), name[ResourcesStatusResponse])
  )

  private val readme: Readme = Readme(ResourceFetcher.getResourceAsString("sequence-manager-service/README.md"))

  val service: Service = Service(
    `http-contract` = Contract(httpEndpoints, httpRequests),
    `websocket-contract` = Contract.empty,
    models = models,
    readme = readme
  )
}

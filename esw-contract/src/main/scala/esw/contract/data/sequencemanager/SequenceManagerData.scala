package esw.contract.data.sequencemanager

import csw.location.api.models.ComponentType.{Machine, SequenceComponent, Sequencer}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, Metadata}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, IRIS, TCS, WFOS}
import esw.ocs.api.models.{ObsMode, SequencerId}
import esw.sm.api.models
import esw.sm.api.models.*
import esw.sm.api.models.ObsModeStatus.{Configurable, Configured, NonConfigurable}
import esw.sm.api.protocol.*
import esw.sm.api.protocol.CommonFailure.LocationServiceError
import esw.sm.api.protocol.ConfigureResponse.*
import esw.sm.api.protocol.ProvisionResponse.{CouldNotFindMachines, SpawningSequenceComponentsFailed, Success}
import esw.sm.api.protocol.SequenceManagerRequest.*
import esw.sm.api.protocol.StartSequencerResponse.*

import java.net.URI

/**
 * This object contains all the Sequence Manager data models which will be sent on wire.
 */
trait SequenceManagerData {
  private val agentPrefix               = Prefix(ESW, "agent")
  private val seqCompPrefix             = Prefix(ESW, "seq_comp")
  val eswSequencerPrefix: Prefix        = Prefix(ESW, "DarkNight")
  val irisSequencerPrefix: Prefix       = Prefix(IRIS, "DarkNight")
  val obsMode: ObsMode                  = ObsMode("DarkNight")
  val irisResource: Resource            = Resource(IRIS)
  val tcsResource: Resource             = Resource(TCS)
  val eswResource: Resource             = Resource(ESW)
  val wfosResource: Resource            = Resource(WFOS)
  val eswSequencerId: SequencerId       = SequencerId(ESW, Some("Variation1"))
  val darkNightSequencers: Sequencers   = Sequencers(eswSequencerPrefix, Prefix(TCS, "DarkNight"))
  val darkNight2Sequencers: Sequencers  = Sequencers(eswSequencerPrefix)
  val darkNight3Sequencers: Sequencers  = Sequencers(eswSequencerPrefix, Prefix(WFOS, "DarkNight"))
  val sequencerComponentId: ComponentId = ComponentId(eswSequencerPrefix, Sequencer)
  val agentComponentId: ComponentId     = ComponentId(agentPrefix, Machine)
  val seqCompComponentId: ComponentId   = ComponentId(seqCompPrefix, SequenceComponent)
  val akkaLocation: AkkaLocation =
    AkkaLocation(AkkaConnection(seqCompComponentId), new URI("uri"), Metadata().add("key1", "value"))
  val agentProvisionConfig: AgentProvisionConfig = AgentProvisionConfig(agentPrefix, 3)
  val provisionConfig: ProvisionConfig           = ProvisionConfig(List(agentProvisionConfig))

  val configure: Configure                                              = Configure(obsMode)
  val provision: Provision                                              = Provision(provisionConfig)
  val getObsModesDetails: GetObsModesDetails.type                       = GetObsModesDetails
  val startSequencer: StartSequencer                                    = StartSequencer(eswSequencerPrefix)
  val restartSequencer: RestartSequencer                                = RestartSequencer(eswSequencerPrefix)
  val shutdownSequencer: ShutdownSequencer                              = ShutdownSequencer(eswSequencerPrefix)
  val shutdownSubsystemSequencers: ShutdownSubsystemSequencers          = ShutdownSubsystemSequencers(ESW)
  val shutdownObsModeSequencers: ShutdownObsModeSequencers              = ShutdownObsModeSequencers(obsMode)
  val shutdownAllSequencers: ShutdownAllSequencers.type                 = ShutdownAllSequencers
  val shutdownSequenceComponent: ShutdownSequenceComponent              = ShutdownSequenceComponent(seqCompPrefix)
  val shutdownAllSequenceComponents: ShutdownAllSequenceComponents.type = ShutdownAllSequenceComponents
  val configureSuccess: ConfigureResponse.Success                       = ConfigureResponse.Success(sequencerComponentId)
  val configurationMissing: ConfigurationMissing                        = ConfigurationMissing(obsMode)
  val conflictingResourcesWithRunningObsMode: ConflictingResourcesWithRunningObsMode = ConflictingResourcesWithRunningObsMode(
    Set(obsMode)
  )
  val failedToStartSequencers: FailedToStartSequencers = FailedToStartSequencers(Set("reason"))
  val couldNotFindMachines: CouldNotFindMachines       = CouldNotFindMachines(Set(agentPrefix))
  val spawningSequenceComponentsFailed: SpawningSequenceComponentsFailed = SpawningSequenceComponentsFailed(
    List("failed sequence component")
  )
  val provisionSuccess: ProvisionResponse.Success.type = Success
  val configuredObsMode: ObsModeDetails =
    models.ObsModeDetails(ObsMode("DarkNight_1"), Configured, Resources(eswResource, tcsResource), darkNightSequencers)
  val configurableObsMode: ObsModeDetails =
    models.ObsModeDetails(ObsMode("DarkNight_2"), Configurable, Resources(eswResource, irisResource), darkNight2Sequencers)
  val nonConfigurableObsMode: ObsModeDetails =
    models.ObsModeDetails(
      ObsMode("DarkNight_3"),
      NonConfigurable(List(irisSequencerPrefix)),
      Resources(eswResource, irisResource, wfosResource),
      darkNightSequencers
    )
  val ObsModesDetailsSuccess: ObsModesDetailsResponse.Success = ObsModesDetailsResponse.Success(
    Set(configuredObsMode, configurableObsMode, nonConfigurableObsMode)
  )
  val alreadyRunning: AlreadyRunning                                                   = AlreadyRunning(sequencerComponentId)
  val started: Started                                                                 = Started(sequencerComponentId)
  val restartSequencerSuccess: RestartSequencerResponse.Success                        = RestartSequencerResponse.Success(sequencerComponentId)
  val shutdownSequencerSuccess: ShutdownSequencersResponse.Success.type                = ShutdownSequencersResponse.Success
  val shutdownSequenceComponentSuccess: ShutdownSequenceComponentResponse.Success.type = ShutdownSequenceComponentResponse.Success
  val loadScriptError: LoadScriptError                                                 = LoadScriptError("error")
  val locationServiceError: LocationServiceError                                       = LocationServiceError("location service error")
  val sequenceComponentNotAvailable: SequenceComponentNotAvailable = SequenceComponentNotAvailable(
    List(eswSequencerPrefix)
  )
  val unhandled: Unhandled = Unhandled("state", "messageType")

  def failedResponse(sequenceManagerRequest: String): FailedResponse =
    FailedResponse(
      s"Sequence Manager Operation($sequenceManagerRequest) failed due to: Ask timed out after [10000] ms"
    )

  val getResourcesStatus: SequenceManagerRequest.GetResources.type = GetResources

  val resourcesStatusSuccess: ResourcesStatusResponse.Success =
    ResourcesStatusResponse.Success(
      List(ResourceStatusResponse(irisResource), ResourceStatusResponse(tcsResource, ResourceStatus.InUse, Some(obsMode)))
    )
  val resourcesStatusFailed: ResourcesStatusResponse.Failed = ResourcesStatusResponse.Failed("error")
}

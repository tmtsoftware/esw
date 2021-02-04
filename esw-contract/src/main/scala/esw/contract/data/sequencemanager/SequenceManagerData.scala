package esw.contract.data.sequencemanager

import csw.location.api.models.ComponentType.{Machine, SequenceComponent, Sequencer}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, Metadata}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, IRIS, TCS, WFOS}
import esw.sm.api.models.ObsModeStatus.{Configurable, Configured, NonConfigurable}
import esw.ocs.api.models.ObsMode
import esw.sm.api.models
import esw.sm.api.models._
import esw.sm.api.protocol.CommonFailure.LocationServiceError
import esw.sm.api.protocol.ConfigureResponse.{ConflictingResourcesWithRunningObsMode, FailedToStartSequencers, _}
import esw.sm.api.protocol.ProvisionResponse.{CouldNotFindMachines, SpawningSequenceComponentsFailed, Success}
import esw.sm.api.protocol.SequenceManagerRequest.{GetObsModesDetails, _}
import esw.sm.api.protocol.StartSequencerResponse._
import esw.sm.api.protocol._

import java.net.URI

trait SequenceManagerData {
  private val agentPrefix               = Prefix(ESW, "agent")
  private val seqCompPrefix             = Prefix(ESW, "seq_comp")
  val sequencerPrefix: Prefix           = Prefix(ESW, "DarkNight")
  val obsMode: ObsMode                  = ObsMode("DarkNight")
  val irisResource: Resource            = Resource(IRIS)
  val tcsResource: Resource             = Resource(TCS)
  val eswResource: Resource             = Resource(ESW)
  val wfosResource: Resource            = Resource(WFOS)
  val sequencerComponentId: ComponentId = ComponentId(sequencerPrefix, Sequencer)
  val agentComponentId: ComponentId     = ComponentId(agentPrefix, Machine)
  val seqCompComponentId: ComponentId   = ComponentId(seqCompPrefix, SequenceComponent)
  val akkaLocation: AkkaLocation =
    AkkaLocation(AkkaConnection(seqCompComponentId), new URI("uri"), Metadata().add("key1", "value"))
  val sequenceComponentStatus: SequenceComponentStatus = SequenceComponentStatus(seqCompComponentId, Some(akkaLocation))
  val agentSeqCompsStatus: AgentStatus                 = AgentStatus(agentComponentId, List(sequenceComponentStatus))
  val agentProvisionConfig: AgentProvisionConfig       = AgentProvisionConfig(agentPrefix, 3)
  val provisionConfig: ProvisionConfig                 = ProvisionConfig(List(agentProvisionConfig))

  val configure: Configure                                              = Configure(obsMode)
  val provision: Provision                                              = Provision(provisionConfig)
  val getObsModesDetails: GetObsModesDetails.type                       = GetObsModesDetails
  val startSequencer: StartSequencer                                    = StartSequencer(ESW, obsMode)
  val restartSequencer: RestartSequencer                                = RestartSequencer(ESW, obsMode)
  val shutdownSequencer: ShutdownSequencer                              = ShutdownSequencer(ESW, obsMode)
  val shutdownSubsystemSequencers: ShutdownSubsystemSequencers          = ShutdownSubsystemSequencers(ESW)
  val shutdownObsModeSequencers: ShutdownObsModeSequencers              = ShutdownObsModeSequencers(obsMode)
  val shutdownAllSequencers: ShutdownAllSequencers.type                 = ShutdownAllSequencers
  val shutdownSequenceComponent: ShutdownSequenceComponent              = ShutdownSequenceComponent(seqCompPrefix)
  val shutdownAllSequenceComponents: ShutdownAllSequenceComponents.type = ShutdownAllSequenceComponents
  val getAgentStatus: SequenceManagerRequest.GetAgentStatus.type        = GetAgentStatus
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
    models.ObsModeDetails(ObsMode("DarkNight_1"), Configured, Resources(eswResource, tcsResource))
  val configurableObsMode: ObsModeDetails =
    models.ObsModeDetails(ObsMode("DarkNight_2"), Configurable, Resources(eswResource, irisResource))
  val nonConfigurableObsMode: ObsModeDetails =
    models.ObsModeDetails(ObsMode("DarkNight_3"), NonConfigurable, Resources(eswResource, irisResource, wfosResource))
  val ObsModesDetailsSuccess: ObsModesDetailsResponse.Success = ObsModesDetailsResponse.Success(
    Set(configuredObsMode, configurableObsMode, nonConfigurableObsMode)
  )
  val alreadyRunning: AlreadyRunning                                                   = AlreadyRunning(sequencerComponentId)
  val started: Started                                                                 = Started(sequencerComponentId)
  val restartSequencerSuccess: RestartSequencerResponse.Success                        = RestartSequencerResponse.Success(sequencerComponentId)
  val shutdownSequencerSuccess: ShutdownSequencersResponse.Success.type                = ShutdownSequencersResponse.Success
  val shutdownSequenceComponentSuccess: ShutdownSequenceComponentResponse.Success.type = ShutdownSequenceComponentResponse.Success
  val agentStatusSuccess: AgentStatusResponse.Success =
    AgentStatusResponse.Success(List(agentSeqCompsStatus), List(sequenceComponentStatus))
  val loadScriptError: LoadScriptError                             = LoadScriptError("error")
  val locationServiceError: LocationServiceError                   = LocationServiceError("location service error")
  val sequenceComponentNotAvailable: SequenceComponentNotAvailable = SequenceComponentNotAvailable(List(ESW))
  val unhandled: Unhandled                                         = Unhandled("state", "messageType")

  val getResourcesStatus: SequenceManagerRequest.GetResources.type = GetResources

  val resourcesStatusSuccess: ResourcesStatusResponse.Success =
    ResourcesStatusResponse.Success(
      List(ResourceStatusResponse(irisResource), ResourceStatusResponse(tcsResource, ResourceStatus.InUse, Some(obsMode)))
    )
  val resourcesStatusFailed: ResourcesStatusResponse.Failed = ResourcesStatusResponse.Failed("error")
}

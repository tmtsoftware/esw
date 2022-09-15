/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.contract.data.sequencemanager

import csw.location.api.models.ComponentType.{Machine, SequenceComponent, Sequencer}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, Metadata}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, IRIS, TCS, WFOS}
import esw.ocs.api.models.{ObsMode, Variation, VariationInfo}
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
  val obsMode: ObsMode                  = ObsMode("DarkNight")
  private val agentPrefix               = Prefix(ESW, "agent")
  private val seqCompPrefix             = Prefix(ESW, "seq_comp")
  val eswSequencerPrefix: Prefix        = Prefix(ESW, obsMode.name)
  val irisSequencerPrefix: Prefix       = Prefix(IRIS, obsMode.name)
  val irisResource: Resource            = Resource(IRIS)
  val tcsResource: Resource             = Resource(TCS)
  val eswResource: Resource             = Resource(ESW)
  val wfosResource: Resource            = Resource(WFOS)
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
  val redVariation: Variation                                           = Variation("red")
  val startSequencer: StartSequencer                                    = StartSequencer(ESW, obsMode, Some(redVariation))
  val startSequencerWithoutVariation: StartSequencer                    = StartSequencer(ESW, obsMode, None)
  val restartSequencer: RestartSequencer                                = RestartSequencer(ESW, obsMode, Some(redVariation))
  val restartSequencerWithoutVariation: RestartSequencer                = RestartSequencer(ESW, obsMode, None)
  val shutdownSequencer: ShutdownSequencer                              = ShutdownSequencer(ESW, obsMode, Some(redVariation))
  val shutdownSequencerWithoutVariation: ShutdownSequencer              = ShutdownSequencer(ESW, obsMode, None)
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
  val variation: Variation                             = Variation("red")
  val variationInfo: VariationInfo                     = VariationInfo(ESW, Some(variation))
  val darkNight1ObsMode: ObsMode                       = ObsMode("DarkNight_1")
  val darkNight2ObsMode: ObsMode                       = ObsMode("DarkNight_2")
  val darkNight3ObsMode: ObsMode                       = ObsMode("DarkNight_3")
  val configuredObsMode: ObsModeDetails =
    models.ObsModeDetails(
      darkNight2ObsMode,
      Configured,
      Resources(eswResource, tcsResource),
      VariationInfos(VariationInfo(ESW), VariationInfo(TCS, Some(variation)))
    )

  val configurableObsMode: ObsModeDetails =
    models.ObsModeDetails(
      darkNight2ObsMode,
      Configurable,
      Resources(eswResource, irisResource),
      VariationInfos(variationInfo)
    )
  val nonConfigurableObsMode: ObsModeDetails =
    models.ObsModeDetails(
      darkNight3ObsMode,
      NonConfigurable(VariationInfos(VariationInfo(IRIS, Some(redVariation)))),
      Resources(eswResource, irisResource, wfosResource),
      VariationInfos(VariationInfo(ESW), VariationInfo(IRIS, Some(redVariation)))
    )
  val ObsModesDetailsSuccess: ObsModesDetailsResponse.Success = ObsModesDetailsResponse.Success(
    Set(configuredObsMode, configurableObsMode, nonConfigurableObsMode)
  )
  val alreadyRunning: AlreadyRunning                                    = AlreadyRunning(sequencerComponentId)
  val started: Started                                                  = Started(sequencerComponentId)
  val restartSequencerSuccess: RestartSequencerResponse.Success         = RestartSequencerResponse.Success(sequencerComponentId)
  val shutdownSequencerSuccess: ShutdownSequencersResponse.Success.type = ShutdownSequencersResponse.Success
  val shutdownSequenceComponentSuccess: ShutdownSequenceComponentResponse.Success.type = ShutdownSequenceComponentResponse.Success
  val loadScriptError: LoadScriptError                                                 = LoadScriptError("error")
  val locationServiceError: LocationServiceError = LocationServiceError("location service error")
  val sequenceComponentNotAvailable: SequenceComponentNotAvailable = SequenceComponentNotAvailable(
    VariationInfos(variationInfo)
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

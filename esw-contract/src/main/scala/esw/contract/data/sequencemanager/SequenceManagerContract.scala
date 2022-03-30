/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.contract.data.sequencemanager

import csw.contract.ResourceFetcher
import csw.contract.generator.*
import csw.contract.generator.ClassNameHelpers.*
import csw.prefix.models.Subsystem
import esw.sm.api.codecs.SequenceManagerServiceCodecs
import esw.sm.api.models.*
import esw.sm.api.protocol.*
import esw.sm.api.protocol.SequenceManagerRequest.*

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
      unhandled,
      failedResponse(name[Configure])
    ),
    ModelType[ProvisionResponse](
      provisionSuccess,
      couldNotFindMachines,
      spawningSequenceComponentsFailed,
      unhandled,
      failedResponse(name[Provision])
    ),
    ModelType[ObsModeDetails](configuredObsMode),
    ModelType[ObsModeStatus](
      ObsModeStatus.Configurable,
      ObsModeStatus.Configured,
      ObsModeStatus.NonConfigurable(VariationInfos(variationInfo))
    ),
    ModelType[ObsModesDetailsResponse](ObsModesDetailsSuccess, locationServiceError),
    ModelType[StartSequencerResponse](
      alreadyRunning,
      started,
      loadScriptError,
      sequenceComponentNotAvailable,
      locationServiceError,
      unhandled,
      failedResponse(name[StartSequencer])
    ),
    ModelType[RestartSequencerResponse](
      restartSequencerSuccess,
      loadScriptError,
      locationServiceError,
      unhandled,
      failedResponse(name[RestartSequencer])
    ),
    ModelType[ShutdownSequencersResponse](
      shutdownSequencerSuccess,
      locationServiceError,
      unhandled,
      failedResponse(name[ShutdownSequencer])
    ),
    ModelType[ShutdownSequenceComponentResponse](
      shutdownSequenceComponentSuccess,
      locationServiceError,
      unhandled,
      failedResponse(name[ShutdownSequenceComponent])
    ),
    ModelType(eswSequencerPrefix),
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
    requestType(startSequencer, startSequencerWithoutVariation)
    requestType(restartSequencer, restartSequencerWithoutVariation)
    requestType(shutdownSequencer, shutdownSequencerWithoutVariation)
    requestType(shutdownSubsystemSequencers)
    requestType(shutdownObsModeSequencers)
    requestType(shutdownAllSequencers)
    requestType(shutdownSequenceComponent)
    requestType(shutdownAllSequenceComponents)
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

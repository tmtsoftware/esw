/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.sm.api.client

import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.models.{ObsMode, Variation}
import esw.sm.api.SequenceManagerApi
import esw.sm.api.codecs.SequenceManagerServiceCodecs
import esw.sm.api.models.ProvisionConfig
import esw.sm.api.protocol.*
import esw.sm.api.protocol.SequenceManagerRequest.*
import msocket.api.Transport

import scala.concurrent.Future

/**
 * HTTP client for the sequence manager
 *
 * @param postClient - An Transport class for HTTP calls for the Sequence Manager
 */
class SequenceManagerClient(postClient: Transport[SequenceManagerRequest])
    extends SequenceManagerApi
    with SequenceManagerServiceCodecs {

  override def configure(obsMode: ObsMode): Future[ConfigureResponse] =
    postClient.requestResponse[ConfigureResponse](Configure(obsMode))

  override def provision(config: ProvisionConfig): Future[ProvisionResponse] =
    postClient.requestResponse[ProvisionResponse](Provision(config))

  override def getObsModesDetails: Future[ObsModesDetailsResponse] =
    postClient.requestResponse[ObsModesDetailsResponse](GetObsModesDetails)

  override def startSequencer(
      subsystem: Subsystem,
      obsMode: ObsMode,
      variation: Option[Variation]
  ): Future[StartSequencerResponse] =
    postClient.requestResponse[StartSequencerResponse](StartSequencer(subsystem, obsMode, variation))

  override def restartSequencer(
      subsystem: Subsystem,
      obsMode: ObsMode,
      variation: Option[Variation]
  ): Future[RestartSequencerResponse] =
    postClient.requestResponse[RestartSequencerResponse](RestartSequencer(subsystem, obsMode, variation))

  override def shutdownSequencer(
      subsystem: Subsystem,
      obsMode: ObsMode,
      variation: Option[Variation]
  ): Future[ShutdownSequencersResponse] =
    postClient.requestResponse[ShutdownSequencersResponse](ShutdownSequencer(subsystem, obsMode, variation))

  override def shutdownSubsystemSequencers(subsystem: Subsystem): Future[ShutdownSequencersResponse] =
    postClient.requestResponse[ShutdownSequencersResponse](ShutdownSubsystemSequencers(subsystem))

  override def shutdownObsModeSequencers(obsMode: ObsMode): Future[ShutdownSequencersResponse] =
    postClient.requestResponse[ShutdownSequencersResponse](ShutdownObsModeSequencers(obsMode))

  override def shutdownAllSequencers(): Future[ShutdownSequencersResponse] =
    postClient.requestResponse[ShutdownSequencersResponse](ShutdownAllSequencers)

  override def shutdownSequenceComponent(prefix: Prefix): Future[ShutdownSequenceComponentResponse] =
    postClient.requestResponse[ShutdownSequenceComponentResponse](ShutdownSequenceComponent(prefix))

  override def shutdownAllSequenceComponents(): Future[ShutdownSequenceComponentResponse] =
    postClient.requestResponse[ShutdownSequenceComponentResponse](ShutdownAllSequenceComponents)

  override def getResources: Future[ResourcesStatusResponse] = postClient.requestResponse[ResourcesStatusResponse](GetResources)
}

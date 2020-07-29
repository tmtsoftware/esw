package esw.sm.api.client

import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.models.ObsMode
import esw.sm.api.SequenceManagerApi
import esw.sm.api.codecs.SequenceManagerHttpCodec
import esw.sm.api.models.ProvisionConfig
import esw.sm.api.protocol.SequenceManagerPostRequest._
import esw.sm.api.protocol._
import msocket.api.Transport

import scala.concurrent.Future

class SequenceManagerClient(postClient: Transport[SequenceManagerPostRequest])
    extends SequenceManagerApi
    with SequenceManagerHttpCodec {

  override def configure(obsMode: ObsMode): Future[ConfigureResponse] =
    postClient.requestResponse[ConfigureResponse](Configure(obsMode))

  override def provision(config: ProvisionConfig): Future[ProvisionResponse] =
    postClient.requestResponse[ProvisionResponse](Provision(config))

  override def getRunningObsModes: Future[GetRunningObsModesResponse] =
    postClient.requestResponse[GetRunningObsModesResponse](GetRunningObsModes)

  override def startSequencer(subsystem: Subsystem, obsMode: ObsMode): Future[StartSequencerResponse] =
    postClient.requestResponse[StartSequencerResponse](StartSequencer(subsystem, obsMode))

  override def restartSequencer(subsystem: Subsystem, obsMode: ObsMode): Future[RestartSequencerResponse] =
    postClient.requestResponse[RestartSequencerResponse](RestartSequencer(subsystem, obsMode))

  override def shutdownSequencer(subsystem: Subsystem, obsMode: ObsMode): Future[ShutdownSequencersResponse] =
    postClient.requestResponse[ShutdownSequencersResponse](ShutdownSequencer(subsystem, obsMode))

  override def shutdownSubsystemSequencers(subsystem: Subsystem): Future[ShutdownSequencersResponse] =
    postClient.requestResponse[ShutdownSequencersResponse](ShutdownSubsystemSequencers(subsystem))

  override def shutdownObsModeSequencers(obsMode: ObsMode): Future[ShutdownSequencersResponse] =
    postClient.requestResponse[ShutdownSequencersResponse](ShutdownObsModeSequencers(obsMode))

  override def shutdownAllSequencers(): Future[ShutdownSequencersResponse] =
    postClient.requestResponse[ShutdownSequencersResponse](ShutdownAllSequencers)

  override def spawnSequenceComponent(machine: Prefix, sequenceComponentName: String): Future[SpawnSequenceComponentResponse] =
    postClient.requestResponse[SpawnSequenceComponentResponse](SpawnSequenceComponent(machine, sequenceComponentName))

  override def shutdownSequenceComponent(prefix: Prefix): Future[ShutdownSequenceComponentResponse] =
    postClient.requestResponse[ShutdownSequenceComponentResponse](ShutdownSequenceComponent(prefix))

  override def shutdownAllSequenceComponents(): Future[ShutdownSequenceComponentResponse] =
    postClient.requestResponse[ShutdownSequenceComponentResponse](ShutdownAllSequenceComponents)

  override def getAgentStatus: Future[AgentStatusResponse] = postClient.requestResponse[AgentStatusResponse](GetAgentStatus)
}

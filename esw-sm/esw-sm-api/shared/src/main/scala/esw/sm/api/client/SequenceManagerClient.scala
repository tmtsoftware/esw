package esw.sm.api.client

import csw.location.api.models.ComponentId
import csw.prefix.models.Subsystem
import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.models.ObsMode
import esw.sm.api.SequenceManagerApi
import esw.sm.api.codecs.SequenceManagerHttpCodec
import esw.sm.api.protocol.SequenceManagerPostRequest._
import esw.sm.api.protocol._
import msocket.api.Transport

import scala.concurrent.Future

class SequenceManagerClient(postClient: Transport[SequenceManagerPostRequest])
    extends SequenceManagerApi
    with SequenceManagerHttpCodec {

  override def configure(obsMode: ObsMode): Future[ConfigureResponse] =
    postClient.requestResponse[ConfigureResponse](Configure(obsMode))

  override def cleanup(obsMode: ObsMode): Future[CleanupResponse] =
    postClient.requestResponse[CleanupResponse](Cleanup(obsMode))

  override def getRunningObsModes: Future[GetRunningObsModesResponse] =
    postClient.requestResponse[GetRunningObsModesResponse](GetRunningObsModes)

  override def startSequencer(subsystem: Subsystem, obsMode: ObsMode): Future[StartSequencerResponse] =
    postClient.requestResponse[StartSequencerResponse](StartSequencer(subsystem, obsMode))

  override def shutdownSequencer(
      subsystem: Subsystem,
      obsMode: ObsMode,
      shutdownSequenceComp: Boolean = false
  ): Future[ShutdownSequencerResponse] =
    postClient.requestResponse[ShutdownSequencerResponse](ShutdownSequencer(subsystem, obsMode, shutdownSequenceComp))

  override def restartSequencer(subsystem: Subsystem, obsMode: ObsMode): Future[RestartSequencerResponse] =
    postClient.requestResponse[RestartSequencerResponse](RestartSequencer(subsystem, obsMode))

  override def shutdownAllSequencers(): Future[ShutdownAllSequencersResponse] =
    postClient.requestResponse[ShutdownAllSequencersResponse](ShutdownAllSequencers)

  override def spawnSequenceComponent(machine: ComponentId, name: String): Future[SpawnSequenceComponentResponse] =
    postClient.requestResponse[SpawnSequenceComponentResponse](SpawnSequenceComponent(machine, name))

  override def shutdownSequenceComponent(prefix: Prefix): Future[ShutdownSequenceComponentResponse] =
    postClient.requestResponse[ShutdownSequenceComponentResponse](ShutdownSequenceComponent(prefix))
}

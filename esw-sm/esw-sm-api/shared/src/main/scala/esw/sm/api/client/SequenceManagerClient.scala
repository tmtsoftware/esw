package esw.sm.api.client

import csw.prefix.models.Subsystem
import esw.sm.api.SequenceManagerApi
import esw.sm.api.codecs.SequenceManagerHttpCodec
import esw.sm.api.protocol.SequenceManagerPostRequest._
import esw.sm.api.protocol.SequenceManagerWebsocketRequest.Configure
import esw.sm.api.protocol._
import msocket.api.Transport

import scala.concurrent.Future

class SequenceManagerClient(
    postClient: Transport[SequenceManagerPostRequest],
    websocketClient: Transport[SequenceManagerWebsocketRequest]
) extends SequenceManagerApi
    with SequenceManagerHttpCodec {

  override def configure(obsMode: String): Future[ConfigureResponse] =
    websocketClient.requestResponse[ConfigureResponse](Configure(obsMode))

  override def cleanup(obsMode: String): Future[CleanupResponse] =
    postClient.requestResponse[CleanupResponse](Cleanup(obsMode))

  override def getRunningObsModes: Future[GetRunningObsModesResponse] =
    postClient.requestResponse[GetRunningObsModesResponse](GetRunningObsModes)

  override def startSequencer(subsystem: Subsystem, obsMode: String): Future[StartSequencerResponse] =
    postClient.requestResponse[StartSequencerResponse](StartSequencer(subsystem, obsMode))

  override def shutdownSequencer(
      subsystem: Subsystem,
      obsMode: String,
      shutdownSequenceComp: Boolean = false
  ): Future[ShutdownSequencerResponse] =
    postClient.requestResponse[ShutdownSequencerResponse](ShutdownSequencer(subsystem, obsMode, shutdownSequenceComp))

  override def restartSequencer(subsystem: Subsystem, obsMode: String): Future[RestartSequencerResponse] =
    postClient.requestResponse[RestartSequencerResponse](RestartSequencer(subsystem, obsMode))

  override def shutdownAllSequencers(): Future[ShutdownAllSequencersResponse] =
    postClient.requestResponse[ShutdownAllSequencersResponse](ShutdownAllSequencers)
}

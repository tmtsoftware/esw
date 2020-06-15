package esw.sm.api

import akka.util.Timeout
import csw.prefix.models.Subsystem
import esw.sm.api.protocol._

import scala.concurrent.Future

trait SequenceManagerApi {
  def configure(observingMode: String)(implicit timeout: Timeout): Future[ConfigureResponse]
  def cleanup(observingMode: String): Future[CleanupResponse]
  def getRunningObsModes: Future[GetRunningObsModesResponse]
  def startSequencer(subsystem: Subsystem, observingMode: String): Future[StartSequencerResponse]
  def shutdownSequencer(
      subsystem: Subsystem,
      observingMode: String,
      shutdownSequenceComp: Boolean = false
  ): Future[ShutdownSequencerResponse]
  def restartSequencer(subsystem: Subsystem, observingMode: String): Future[RestartSequencerResponse]
  def shutdownAllSequencers(): Future[ShutdownAllSequencersResponse]
}

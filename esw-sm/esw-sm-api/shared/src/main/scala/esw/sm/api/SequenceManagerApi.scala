package esw.sm.api

import csw.prefix.models.Subsystem
import esw.sm.api.models.{CleanupResponse, ConfigureResponse, GetRunningObsModesResponse, StartSequencerResponse}

import scala.concurrent.Future

trait SequenceManagerApi {
  def configure(observingMode: String): Future[ConfigureResponse]
  def cleanup(observingMode: String): Future[CleanupResponse]
  def getRunningObsModes: Future[GetRunningObsModesResponse]
  def startSequencer(subsystem: Subsystem, observingMode: String): Future[StartSequencerResponse]
}

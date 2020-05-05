package esw.sm.api

import esw.sm.api.models.{CleanupResponse, ConfigureResponse, GetRunningObsModesResponse}

import scala.concurrent.Future

trait SequenceManagerApi {
  def configure(observingMode: String): Future[ConfigureResponse]
  def cleanup(observingMode: String): Future[CleanupResponse]
  def getRunningObsModes: Future[GetRunningObsModesResponse]
}

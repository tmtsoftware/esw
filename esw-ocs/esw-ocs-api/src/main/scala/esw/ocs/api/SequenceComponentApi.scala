package esw.ocs.api

import esw.ocs.api.models.responses.SequenceComponentResponse.{Done, GetStatusResponse, LoadScriptResponse}

import scala.concurrent.Future

trait SequenceComponentApi {
  def loadScript(sequencerId: String, observingMode: String): Future[LoadScriptResponse]
  def getStatus: Future[GetStatusResponse]

  def unloadScript(): Future[Done.type]
}

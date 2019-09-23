package esw.ocs.api

import akka.Done
import esw.ocs.api.protocol.{GetStatusResponse, LoadScriptResponse}

import scala.concurrent.Future

trait SequenceComponentApi {
  def loadScript(sequencerId: String, observingMode: String): Future[LoadScriptResponse]
  def unloadScript(): Future[Done]
  def status: Future[GetStatusResponse]
}

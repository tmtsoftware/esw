package esw.ocs.api

import akka.Done
import csw.prefix.models.Subsystem
import esw.ocs.api.protocol.{GetStatusResponse, LoadScriptResponse, RestartScriptResponse}

import scala.concurrent.Future

trait SequenceComponentApi {
  def loadScript(subsystem: Subsystem, observingMode: String): Future[LoadScriptResponse]
  def restart(): Future[RestartScriptResponse]
  def unloadScript(): Future[Done]
  def status: Future[GetStatusResponse]
}

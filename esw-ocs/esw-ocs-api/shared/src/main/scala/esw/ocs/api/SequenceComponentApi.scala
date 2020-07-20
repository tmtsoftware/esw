package esw.ocs.api

import csw.prefix.models.Subsystem
import esw.ocs.api.models.ObsMode
import esw.ocs.api.protocol.SequenceComponentResponse.{GetStatusResponse, Ok, ScriptResponseOrUnhandled}

import scala.concurrent.Future

trait SequenceComponentApi {
  def loadScript(subsystem: Subsystem, obsMode: ObsMode): Future[ScriptResponseOrUnhandled]
  def restartScript(): Future[ScriptResponseOrUnhandled]
  def unloadScript(): Future[Ok.type]
  def status: Future[GetStatusResponse]
  def shutdown(): Future[Ok.type]
}
